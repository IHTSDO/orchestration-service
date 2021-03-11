package org.ihtsdo.orchestration.dao;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;

import java.util.Set;

public class TermserverReleaseRequestPojo {

	private String productName;
	private String effectiveDate;
	private SnowstormRestClient.ExportCategory exportCategory;
	private String branchPath;
	private String releaseCenter;
	private Set<String> excludedModuleIds;
	private String authToken;
	private String failureExportMax;

	public TermserverReleaseRequestPojo() {
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public SnowstormRestClient.ExportCategory getExportCategory() {
		return exportCategory;
	}

	public void setExportCategory(SnowstormRestClient.ExportCategory exportCategory) {
		this.exportCategory = exportCategory;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	public String getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(String releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public Set<String> getExcludedModuleIds() {
		return excludedModuleIds;
	}

	public void setExcludedModuleIds(Set<String> excludedModuleIds) {
		this.excludedModuleIds = excludedModuleIds;
	}
	
	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getFailureExportMax() {
		return failureExportMax;
	}

	public void setFailureExportMax(String failureExportMax) {
		this.failureExportMax = failureExportMax;
	}

	@Override
	public String toString() {
		return "TermserverReleaseRequestPojo{" +
				"productName='" + productName + '\'' +
				", effectiveDate='" + effectiveDate + '\'' +
				", exportCategory=" + exportCategory +
				", branchPath='" + branchPath + '\'' +
				", releaseCenter='" + releaseCenter + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				", failureExportMax=" + failureExportMax +
				'}';
	}
}