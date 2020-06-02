package org.ihtsdo.orchestration.service;

public enum OrchProcStatus {
	SCHEDULED, EXPORTING, BUILD_INITIATING, BUILDING, VALIDATING, COMPLETED, FAILED;

	public static OrchProcStatus[] FINAL_STATES = new OrchProcStatus[] { COMPLETED, FAILED };

	public static boolean isFinalState(String status) {
		for (OrchProcStatus thisStatus : FINAL_STATES) {
			if (thisStatus.name().equals(status)) {
				return true;
			}
		}
		return false;
	}
}
