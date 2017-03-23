package org.ihtsdo.orchestration.service;

public enum OrchProcStatus {
	SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED;

	public static OrchProcStatus[] FINAL_STATES = new OrchProcStatus[] { COMPLETED, FAILED };

	public static boolean isFinalState(String status) {

		for (OrchProcStatus thisStatus : FINAL_STATES) {
			if (status.equals(thisStatus.toString())) {
				return true;
			}
		}
		return false;
	}
}
