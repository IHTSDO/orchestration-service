package org.ihtsdo.orchestration.service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.orchestration.OrchestrationConstants;
import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.ihtsdo.orchestration.clients.snowowl.TerminologyServerRestClientFactory;
import org.ihtsdo.orchestration.dao.FileManager;
import org.ihtsdo.orchestration.dao.OrchestrationProcessReportDAO;
import org.ihtsdo.orchestration.model.JobStatus;
import org.ihtsdo.orchestration.model.StatusAndReportUrl;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.orchestration.rest.ValidationParameterConstants;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.DateUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.google.common.io.Files;

import static org.ihtsdo.orchestration.model.JobStatus.FAILED;
import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory.UNPUBLISHED;
import static org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportType.DELTA;

public class ValidationService implements OrchestrationConstants {

	public static final String VALIDATION_PROCESS = "validation";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected OrchestrationProcessReportDAO processReportDAO;

	@Autowired
	private TerminologyServerRestClientFactory terminologyServerRestClientFactory;

	@Autowired
	protected RVFRestClient rvfClient;
	
	private final ExecutorService executorService;

	public ValidationService() {
		executorService = Executors.newCachedThreadPool();
	}

	public synchronized void validate(ValidationConfiguration validationConfig, String branchPath, String effectiveDate, String authToken, OrchestrationCallback callback) throws EntityAlreadyExistsException {
		Assert.notNull(branchPath);
		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = processReportDAO.getStatus(branchPath, VALIDATION_PROCESS);
		if (status != null && !JobStatus.isFinalState(status)) {
			throw new EntityAlreadyExistsException("An in-progress validation has been detected for " + branchPath + " at state " + status);
		}

		// Update S3 location
		processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ValidationRunner(validationConfig, branchPath, effectiveDate, authToken, callback))).start();

	}

	public ValidationReportDTO getLatestValidation(String path) throws IOException {
		final String status = processReportDAO.getStatus(path, VALIDATION_PROCESS);
		String latestReport = null;
		if (status != null) {
			if (status.equals(JobStatus.COMPLETED.toString())) {
				latestReport = processReportDAO.getLatestValidationReport(path);
			}
			return new ValidationReportDTO(status, latestReport);
		}
		return null;
	}

	public List<String> getLatestValidationStatuses(List<String> paths) throws IOException {
		List<Callable<String>> tasks = new ArrayList<>();
		for (final String path : paths) {
			tasks.add(() -> processReportDAO.getStatus(path, VALIDATION_PROCESS));
		}
		try {
			final List<Future<String>> futures = executorService.invokeAll(tasks);
			final List<String> statuses = new ArrayList<>();
			for (Future<String> future : futures) {
				statuses.add(future.get());
			}
			return statuses;
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException("Failed to load validation statuses.", e);
		}
	}

	private class ValidationRunner implements Runnable {

		private final String branchPath;
		private final String authToken;
		private final OrchestrationCallback callback;
		private final ValidationConfiguration config;

		private ValidationRunner(ValidationConfiguration validationConfig, String branchPath, String effectiveDate, String authToken, OrchestrationCallback callback) {
			this.branchPath = branchPath;
			this.authToken = authToken;
			this.callback = callback;
			config = validationConfig;
			config.setProductName(branchPath.replace("/", "_"));
			if (effectiveDate != null) {
				config.setReleaseDate(effectiveDate);
			} else {
				config.setReleaseDate(DateUtils.now(DateUtils.YYYYMMDD));
			}
		}

		@Override
		public void run() {
			logger.debug("ValidationConfig:{}", config);
			StatusAndReportUrl finalOrchProcStatus = new StatusAndReportUrl(FAILED);
			File exportArchive = null;
			try {
				// check the config is set correctly
				String errorMsg = config.checkMissingParameters();
				if (errorMsg != null) {
					throw new BadRequestException("Validation configuration is not set correctly:" + errorMsg);
				}
				// Export
				processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.EXPORTING.toString(), null);
				//check and update export effective time
				String exportEffectiveTime = resolveExportEffectiveTime(config);

				// Create terminology server client using SSO security token
				SnowstormRestClient snowstormClient = terminologyServerRestClientFactory.getClient(authToken);

				// Export RF2 delta
				final long branchHeadTimestamp = snowstormClient.getBranch(branchPath).getHeadTimestamp();
				exportArchive = snowstormClient.export(branchPath, exportEffectiveTime, null, UNPUBLISHED, DELTA);
				config.setContentHeadTimestamp(branchHeadTimestamp);

				// send delta export directly for RVF validation
				finalOrchProcStatus = validateByRvfDirectly(exportArchive);
			} catch (Exception e) {
				processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, FAILED.toString(), e.getMessage());
				finalOrchProcStatus = new StatusAndReportUrl(FAILED);
				logger.error("Validation of {} failed.", branchPath, e);
			} finally {
				if (callback != null) {
					callback.complete(finalOrchProcStatus);
				}
				FileManager.deleteFileIfExists(exportArchive);
			}
		}

		private String resolveExportEffectiveTime(ValidationConfiguration config) throws ParseException {
			String exportEffectiveDate = config.getReleaseDate();
			String mostRecentRelease = null;
			if (config.getDependencyRelease() != null) {
				mostRecentRelease = config.getDependencyRelease();
			} else if (config.getPreviousRelease() != null) {
				mostRecentRelease = config.getPreviousRelease();
			}
			if (mostRecentRelease != null) {
				String[] splits = mostRecentRelease.split(ValidationParameterConstants.UNDER_SCORE);
				String dateStr = (splits.length == 2) ? splits[1] : splits[0];
				Calendar calendar = new GregorianCalendar();
				SimpleDateFormat formatter = new SimpleDateFormat(DateUtils.YYYYMMDD);
				if (!formatter.parse(config.getReleaseDate()).after(formatter.parse(dateStr))) {
					calendar.setTime(formatter.parse(dateStr));
					calendar.add(Calendar.DAY_OF_YEAR, 1);
					exportEffectiveDate = formatter.format(calendar.getTime());
					logger.debug("The effective date for termServer exporting is set to {} one day after the most recent release {}", exportEffectiveDate, mostRecentRelease);
				}
			}
			return exportEffectiveDate;
		}

		public StatusAndReportUrl validateByRvfDirectly(File exportArchive) throws Exception {
			File tempDir = Files.createTempDir();
			File localZipFile = new File(tempDir, config.getProductName() + "_" + config.getReleaseDate() + ".zip");
			JobStatus status = FAILED;
			String rvfResultUrl = null;
			try {
				// change file name exported to RF2 format
				processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.BUILD_INITIATING.toString(), null);
				// prepare files for validation
				rvfClient.prepareExportFilesForValidation(exportArchive, config, false, localZipFile);
				processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.BUILDING.toString(), null);
				// call validation API
				rvfResultUrl = rvfClient.runValidationForRF2DeltaExport(localZipFile, config);
			} finally {
				FileManager.deleteFileIfExists(localZipFile);
				FileManager.deleteFileIfExists(tempDir);
				FileManager.deleteFileIfExists(exportArchive);
			}
			// polling results
			if (rvfResultUrl != null) {
				processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.VALIDATING.toString(), null);
				JSONObject rvfReport = rvfClient.waitForResponse(rvfResultUrl);
				processReportDAO.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
				Object responseState = rvfReport.get(RVFRestClient.JSON_FIELD_STATUS);
				RVFRestClient.RVF_STATE currentState;
				try {
					currentState = RVFRestClient.RVF_STATE.valueOf(responseState.toString());
				} catch (Exception e) {
					throw new ProcessingException("Failed to determine RVF Status from response: " + rvfReport.toString(RVFRestClient.INDENT));
				}
				if (RVFRestClient.RVF_STATE.COMPLETE.equals(currentState)) {
					processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, JobStatus.COMPLETED.toString(), null);
					status = JobStatus.COMPLETED;
				} else {
					String errorMsg = new JSONObject(rvfReport.get(RVFRestClient.JSON_FIELD_RVF_VALIDATION_RESULT).toString()).get(RVFRestClient.JSON_FIELD_FAILURE_MESSAGES).toString();
					logger.error("Failed to validate. Error: {}", errorMsg);
					processReportDAO.setStatus(branchPath, VALIDATION_PROCESS, FAILED.toString(), errorMsg);
					status = FAILED;
				}
			}
			return new StatusAndReportUrl(status, rvfResultUrl);
		}
	}
}
