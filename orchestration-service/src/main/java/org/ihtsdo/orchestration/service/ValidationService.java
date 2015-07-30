package org.ihtsdo.orchestration.service;

import org.ihtsdo.orchestration.dao.TSDao;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidationService {

	public enum ValidationStatus {
		SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED
	}

	public static final String VALIDATION_PROCESS = "validation";

	@Autowired
	TSDao dao;

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

				// Create files for SRS

				// Initiate SRS

				// Trigger SRS

				// Wait for RVF response


			} catch (Exception e) {
				dao.setStatus(branchPath, VALIDATION_PROCESS, ValidationStatus.FAILED.toString(), e.getMessage());
			}
			
		}
	}

}
