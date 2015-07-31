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
import org.springframework.beans.factory.annotation.Autowired;

public class ValidationService {

	public enum ValidationStatus {
		SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED
	}

	public static final String VALIDATION_PROCESS = "validation";

	@Autowired
	TSDao dao;

	@Autowired
	protected SnowOwlRestClient snowOwlRestClient;

	@Autowired
	SRSRestClient srsClient;

	@Autowired
	RVFRestClient rvfClient;

	public synchronized void validate(String branchPath) throws EntityAlreadyExistsException {

		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = dao.getStatus(branchPath, VALIDATION_PROCESS);
		if (status != null && !status.equals(ValidationStatus.COMPLETED.toString())) {
			throw new EntityAlreadyExistsException("An existing validation has been detected at state " + status.toString());
		}

		// Update S3 location
		dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ValidationRunner(branchPath))).start();

	}
	
	private class ValidationRunner implements Runnable {
		
		String branchPath;
		
		ValidationRunner (String branchPath) {
			this.branchPath = branchPath;
		}

		@Override
		public void run() {
			
			try {
				// Export
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.EXPORTING.toString(), null);
				File exportArchive = snowOwlRestClient.export(branchPath, SnowOwlRestClient.ExtractType.DELTA);

				// Create files for SRS / Initiate SRS
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILD_INITIATING.toString(), null);
				SRSProjectConfiguration config = srsClient.prepareSRSFiles(exportArchive);

				// Trigger SRS
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.BUILDING.toString(), null);
				Map<String, String> srsResponse = srsClient.runDailyBuild(config);

				// Wait for RVF response
				// Did we obtain the RVF location for the next step in the process to poll?
				if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
					dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.VALIDATING.toString(), null);
					JSONObject rvfReport = rvfClient.waitForResponse(srsResponse.get(SRSRestClient.RVF_RESPONSE));
					dao.saveReport(branchPath, VALIDATION_PROCESS, rvfReport);
				} else {
					String error = "Did not find RVF Response location in SRS Client Response";
					dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), error);
				}

			} catch (Exception e) {
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), e.getMessage());
			}
			
		}
	}

}
