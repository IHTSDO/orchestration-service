package org.ihtsdo.orchestration.model;

public class StatusAndReportUrl {

	private final JobStatus status;
	private String reportUrl;

	public StatusAndReportUrl(JobStatus status) {
		this.status = status;
	}

	public StatusAndReportUrl(JobStatus status, String reportUrl) {
		this.status = status;
		this.reportUrl = reportUrl;
	}

	public JobStatus getStatus() {
		return status;
	}

	public String getReportUrl() {
		return reportUrl;
	}
}
