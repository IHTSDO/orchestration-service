package org.ihtsdo.orchestration.model;

public enum JobStatus {
	SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED;

	public static JobStatus[] FINAL_STATES = new JobStatus[] { COMPLETED, FAILED };

	public static boolean isFinalState(String status) {
		for (JobStatus thisStatus : FINAL_STATES) {
			if (thisStatus.name().equals(status)) {
				return true;
			}
		}
		return false;
	}
}
