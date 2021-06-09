package org.ihtsdo.orchestration.service;

import org.ihtsdo.orchestration.model.StatusAndReportUrl;

public interface OrchestrationCallback {
	void complete(StatusAndReportUrl statusUpdate);
}
