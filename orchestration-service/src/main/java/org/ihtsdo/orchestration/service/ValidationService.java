package org.ihtsdo.orchestration.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.dao.OrchProcDAO;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public class ValidationService {

	public static final String README_HEADER_FILENAME = "/readme-header.txt";

	public static final String VALIDATION_PROCESS = "validation";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected OrchProcDAO orchProcDAO;

	@Autowired
	protected SnowOwlRestClient snowOwlRestClient;

	@Autowired
	protected SRSRestClient srsClient;

	@Autowired
	protected RVFRestClient rvfClient;

	private SRSProjectConfiguration defaultConfiguration;

	public ValidationService(SRSProjectConfiguration defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	public void init() throws IOException {
		// Load the readme header from our resource into the default configuration
		// More efficient to do it here than load it each time
		InputStream is = getClass().getResourceAsStream(README_HEADER_FILENAME);
		Assert.notNull(is, "Failed to load readme-header.");
		String readmeHeader = IOUtils.toString(is, StandardCharsets.UTF_8);
		defaultConfiguration.setReadmeHeader(readmeHeader);
		logger.info("Loaded ReadmeHeader from resource file " + README_HEADER_FILENAME);
		IOUtils.closeQuietly(is);
	}

	public synchronized void validate(String branchPath, String effectiveDate) throws EntityAlreadyExistsException {
		validate(branchPath, effectiveDate, false, null);
	}
	
	public synchronized void validate(String branchPath, String effectiveDate, OrchestrationCallback callback) throws EntityAlreadyExistsException {
		//skip srs build for rvf validation but leave the option open in case we need it
		validate(branchPath, effectiveDate, false, callback);
	}

	public synchronized void validate(String branchPath, String effectiveDate, boolean isSrsBuildRequired, OrchestrationCallback callback) throws EntityAlreadyExistsException {
		Assert.notNull(branchPath);
		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = orchProcDAO.getStatus(branchPath, VALIDATION_PROCESS);
		if (status != null && !OrchProcStatus.isFinalState(status)) {
			throw new EntityAlreadyExistsException("An in-progress validation has been detected for " + branchPath + " at state " + status);
		}

		// Update S3 location
		orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ValidationRunner(branchPath, effectiveDate, callback, isSrsBuildRequired))).start();

	}

	public ValidationReportDTO getLatestValidation(String path) throws IOException {
		final String status = orchProcDAO.getStatus(path, VALIDATION_PROCESS);
		String latestReport = null;
		if (status != null) {
			if (status.equals(OrchProcStatus.COMPLETED.toString())) {
				latestReport = orchProcDAO.getLatestValidationReport(path);
			}
			return new ValidationReportDTO(status, latestReport);
		}
		return null;
	}

	public List<String> getLatestValidationStatuses(List<String> paths) {
		List<String> statuses = new ArrayList<>();
		for (String path : paths) {
			statuses.add(orchProcDAO.getStatus(path, VALIDATION_PROCESS));
		}
		return statuses;
	}



	private class ValidationRunner implements Runnable {

		private final String branchPath;
		private final String effectiveDate;
		private final OrchestrationCallback callback;
		private SRSProjectConfiguration config;
		private boolean isSrsBuildRequired;

		private ValidationRunner(String branchPath, String effectiveDate, OrchestrationCallback callback, boolean isSrsBuildRequired) {
			this.branchPath = branchPath;
			this.effectiveDate = effectiveDate;
			//Note that the SRS Release date is determined from the date found in the archive file
			this.callback = callback;
			config = defaultConfiguration.clone();
			config.setProductName(branchPath.replace("/", "_"));
			this.isSrsBuildRequired = isSrsBuildRequired;
		}

		@Override
		public void run() {

			OrchProcStatus finalOrchProcStatus = OrchProcStatus.FAILED;
			try {
				// Export
				orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.EXPORTING.toString(), null);
				File exportArchive = snowOwlRestClient.export(branchPath, effectiveDate, SnowOwlRestClient.ExportType.UNPUBLISHED,
						SnowOwlRestClient.ExtractType.DELTA);
				if (isSrsBuildRequired) {
					 finalOrchProcStatus = validationWithSrsBuild(exportArchive);
				} else {
					//send delta export directly for RVF validation
					finalOrchProcStatus = validateByRvfDirectly(exportArchive);
				}
			} catch (Exception e) {
				orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.FAILED.toString(), e.getMessage());
				logger.error("Validation of {} failed.", branchPath, e);
			}
			callback.complete(finalOrchProcStatus);
		}

		private OrchProcStatus validationWithSrsBuild(File exportArchive)
				throws Exception {
			OrchProcStatus status = OrchProcStatus.FAILED;
			// Create files for SRS / Initiate SRS
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.BUILD_INITIATING.toString(), null);
			boolean includeExternallyMaintainedFiles = false;
			srsClient.prepareSRSFiles(exportArchive, config, includeExternallyMaintainedFiles);
			// Trigger SRS
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.BUILDING.toString(), null);
			srsClient.configureBuild(config);
			Map<String, String> srsResponse = srsClient.runBuild(config);

			// Wait for RVF response
			// Did we obtain the RVF location for the next step in the process to poll?
			if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
				orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.VALIDATING.toString(), null);
				JSONObject rvfReport = rvfClient.waitForResponse(srsResponse.get(SRSRestClient.RVF_RESPONSE));
				orchProcDAO.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
				orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.COMPLETED.toString(), null);
				status = OrchProcStatus.COMPLETED;
			} else {
				String error = "Did not find RVF Response location in SRS Client Response";
				orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.FAILED.toString(), error);
			}
			return status;
		}
		
		public OrchProcStatus validateByRvfDirectly(File exportArchive) throws Exception {
			OrchProcStatus status = OrchProcStatus.FAILED;
			//change file name exported to RF2 format
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.BUILD_INITIATING.toString(), null);
			File zipFile = rvfClient.prepareExportFilesForValidation(exportArchive, config, false);
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.BUILDING.toString(), null);
			//call validation API
			String rvfResultUrl = rvfClient.runValidationForRF2DeltaExport(zipFile, config);
			//polling results
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.VALIDATING.toString(), null);
			JSONObject rvfReport = rvfClient.waitForResponse(rvfResultUrl);
			orchProcDAO.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
			orchProcDAO.setStatus(branchPath, VALIDATION_PROCESS, OrchProcStatus.COMPLETED.toString(), null);
			status = OrchProcStatus.COMPLETED;
			return status;
		}
	}
}
