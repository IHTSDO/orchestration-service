package org.ihtsdo.orchestration.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class ValidationReportDTO {

	private String executionStatus;
	private String reportJson;

	public ValidationReportDTO(String executionStatus, String reportJson) {
		this.executionStatus = executionStatus;
		this.reportJson = reportJson;
	}

	public String getExecutionStatus() {
		return executionStatus;
	}

	@JsonRawValue
	public String getReport() {
		return reportJson;
	}
}
