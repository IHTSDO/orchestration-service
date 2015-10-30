package org.ihtsdo.orchestration.service;

public interface OrchestrationCallback {
	void complete(OrchProcStatus finalStatus);
}
