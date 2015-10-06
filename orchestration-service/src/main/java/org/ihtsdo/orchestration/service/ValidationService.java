package org.ihtsdo.orchestration.service;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.dao.ValidationDAO;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidationService {

	public enum ValidationStatus {
		SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED
	}

	public static final String README_HEADER_FILENAME = "/readme-header.txt";

	public static ValidationStatus[] FINAL_STATES = new ValidationStatus[] { ValidationStatus.COMPLETED, ValidationStatus.FAILED };

	public static final String VALIDATION_PROCESS = "validation";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected ValidationDAO validationDAO;

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
		validate(branchPath, effectiveDate, null);
	}

	public synchronized void validate(String branchPath, String effectiveDate, ValidationCallback callback) throws EntityAlreadyExistsException {
		Assert.notNull(branchPath);
		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = validationDAO.getStatus(branchPath, VALIDATION_PROCESS);
		if (status != null && !isFinalState(status)) {
			throw new EntityAlreadyExistsException("An in-progress validation has been detected for " + branchPath + " at state " + status);
		}

		// Update S3 location
		validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ValidationRunner(branchPath, effectiveDate, callback))).start();

	}

	public ValidationReportDTO getLatestValidation(String path) throws IOException {
		final String status = validationDAO.getStatus(path, VALIDATION_PROCESS);
		String latestReport = null;
		if (status != null) {
			if (status.equals(ValidationStatus.COMPLETED.toString())) {
				latestReport = validationDAO.getLatestReport(path);
			}
			return new ValidationReportDTO(status, latestReport);
		}
		return null;
	}

	public List<String> getLatestValidationStatuses(List<String> paths) {
		List<String> statuses = new ArrayList<>();
		for (String path : paths) {
			statuses.add(validationDAO.getStatus(path, VALIDATION_PROCESS));
		}
		return statuses;
	}

	public static boolean isFinalState(String status) {

		for (ValidationStatus thisStatus : FINAL_STATES) {
			if (status.equals(thisStatus.toString())) {
				return true;
			}
		}
		return false;
	}

	private class ValidationRunner implements Runnable {

		private final String branchPath;
		private final String effectiveDate;
		private final ValidationCallback callback;
		private SRSProjectConfiguration config;

		private ValidationRunner(String branchPath, String effectiveDate, ValidationCallback callback) {
			this.branchPath = branchPath;
			this.effectiveDate = effectiveDate;
			//Note that the SRS Release date is determined from the date found in the archive file
			
			this.callback = callback;
			config = defaultConfiguration.clone();
			config.setProductName(branchPath.replace("/", "_"));
		}

		@Override
		public void run() {

			ValidationStatus finalValidationStatus = ValidationStatus.FAILED;
			try {
				// Export
				validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.EXPORTING.toString(), null);
				File exportArchive = snowOwlRestClient.export(branchPath, effectiveDate, SnowOwlRestClient.ExtractType.DELTA);

				// Create files for SRS / Initiate SRS
				validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILD_INITIATING.toString(), null);
				srsClient.prepareSRSFiles(exportArchive, config);

				// Trigger SRS
				validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILDING.toString(), null);
				Map<String, String> srsResponse = srsClient.runBuild(config);

				// Wait for RVF response
				// Did we obtain the RVF location for the next step in the process to poll?
				if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
					validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.VALIDATING.toString(), null);
					JSONObject rvfReport = rvfClient.waitForResponse(srsResponse.get(SRSRestClient.RVF_RESPONSE));
					validationDAO.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
					validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.COMPLETED.toString(), null);
					finalValidationStatus = ValidationStatus.COMPLETED;
				} else {
					String error = "Did not find RVF Response location in SRS Client Response";
					validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), error);
				}
			} catch (Exception e) {
				validationDAO.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), e.getMessage());
				logger.error("Validation of {} failed.", branchPath, e);
			}
			callback.complete(finalValidationStatus);
		}
	}

}
