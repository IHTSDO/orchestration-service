package org.ihtsdo.orchestration.service;

import java.io.File;
import java.util.Map;

import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.dao.TSDao;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidationService {

	public enum ValidationStatus {
		SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED
	}

	public static ValidationStatus[] FINAL_STATES = new ValidationStatus[] { ValidationStatus.COMPLETED, ValidationStatus.FAILED };

	public static final String VALIDATION_PROCESS = "validation";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected TSDao dao;

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

	public synchronized void validate(String branchPath) throws EntityAlreadyExistsException {

		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = dao.getStatus(branchPath, VALIDATION_PROCESS);
		if (status != null && !isFinalState(status)) {
			throw new EntityAlreadyExistsException("An existing validation has been detected at state " + status.toString());
		}

		// Update S3 location
		dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ValidationRunner(branchPath))).start();

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
		
		String branchPath;
		SRSProjectConfiguration config;
		
		ValidationRunner (String branchPath) {
			this.branchPath = branchPath;
			config = defaultConfiguration.clone();
			config.setProductName(branchPath.replace("/", "_"));
		}

		@Override
		public void run() {
			
			try {
				// Export
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.EXPORTING.toString(), null);
				File exportArchive = snowOwlRestClient.export(branchPath, SnowOwlRestClient.ExtractType.DELTA);

				// Create files for SRS / Initiate SRS
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILD_INITIATING.toString(), null);
				srsClient.prepareSRSFiles(exportArchive, config);

				// Trigger SRS
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILDING.toString(), null);
				Map<String, String> srsResponse = srsClient.runBuild(config);

				// Wait for RVF response
				// Did we obtain the RVF location for the next step in the process to poll?
				if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
					dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.VALIDATING.toString(), null);
					JSONObject rvfReport = rvfClient.waitForResponse(srsResponse.get(SRSRestClient.RVF_RESPONSE));
					dao.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
					dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.COMPLETED.toString(), null);
				} else {
					String error = "Did not find RVF Response location in SRS Client Response";
					dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), error);
				}

			} catch (Exception e) {
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), e.getMessage());
				logger.error("Validation of {} failed.", branchPath, e);
			}
			
		}
	}

}
