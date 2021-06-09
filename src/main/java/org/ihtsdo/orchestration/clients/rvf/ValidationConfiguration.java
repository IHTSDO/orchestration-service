package org.ihtsdo.orchestration.clients.rvf;

public class ValidationConfiguration {

	private String previousRelease;
	private String assertionGroupNames;
	private String rvfDroolsAssertionGroupNames;
	private String failureExportMax = "10";  
	private String dependencyRelease;
	private String productName;
	private String releaseDate;
	private String releaseCenter;
	private String previousPackage;
	private String dependencyPackage;
	private String includedModuleIds;
	private boolean enableMRCMValidation;
	private Long contentHeadTimestamp;

	public String checkMissingParameters() {
		StringBuilder msgBuilder = new StringBuilder();
		if (this.assertionGroupNames == null) {
			msgBuilder.append("assertionGroupNames can't be null.");
		}
		if (dependencyRelease == null && previousRelease == null) {
			if (msgBuilder.length() > 0) {
				msgBuilder.append(" ");
			}
			msgBuilder.append("previousRelease and dependencyRelease can't be both null.");
		} 
		
		if (previousPackage == null && dependencyPackage == null) {
			if (msgBuilder.length() > 0) {
				msgBuilder.append(" ");
			}
			msgBuilder.append("previousPackage and dependencyPackage can't be both null.");
		} 
		if (!msgBuilder.toString().isEmpty()) {
			return msgBuilder.toString();
		}
		return null;
	}
	
	public String getPreviousPackage() {
		return previousPackage;
	}

	public void setPreviousPackage(String previousPackage) {
		this.previousPackage = previousPackage;
	}

	public String getDependencyPackage() {
		return dependencyPackage;
	}

	public void setDependencyPackage(String dependencyPackage) {
		this.dependencyPackage = dependencyPackage;
	}

	public String getDependencyRelease() {
		return dependencyRelease;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}
	
	public String getPreviousRelease() {
		return previousRelease;
	}

	public void setPreviousRelease(String previousRelease) {
		this.previousRelease = previousRelease;
	}

	public String getAssertionGroupNames() {
		return assertionGroupNames;
	}

	public void setAssertionGroupNames(String assertionGroupNames) {
		this.assertionGroupNames = assertionGroupNames;
	}

	public String getFailureExportMax() {
		return failureExportMax;
	}

	public void setFailureExportMax(String failureExportMax) {
		this.failureExportMax = failureExportMax;
	}

	public void setDependencyRelease(String extensionDependencyRelease) {
		this.dependencyRelease = extensionDependencyRelease;
	}

	public String getProductName() {
		return this.productName;
	}

	public String getReleaseDate() {
		return this.releaseDate;
	}

	@Override
	public String toString() {
		return "ValidationConfiguration [productName=" + productName
				+ ", releaseDate=" + releaseDate + ", assertionGroupNames="
				+ assertionGroupNames + ", rvfDroolsAssertionGroupNames="
				+ rvfDroolsAssertionGroupNames + ", failureExportMax="
				+ failureExportMax + ", previousRelease="
				+ previousRelease + ", dependencyRelease="
				+ dependencyRelease + ",releaseCenter="
				+ releaseCenter + ",includedModuleIds="
				+ includedModuleIds + "]";
	}	

	public void setReleaseCenter(String releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public String getReleaseCenter() {
		return this.releaseCenter;
	}

	public String getRvfDroolsAssertionGroupNames() {
		return rvfDroolsAssertionGroupNames;
	}

	public void setRvfDroolsAssertionGroupNames(String rvfDroolsAssertionGroupNames) {
		this.rvfDroolsAssertionGroupNames = rvfDroolsAssertionGroupNames;
	}

	public String getIncludedModuleIds() {
		return includedModuleIds;
	}

	public void setIncludedModuleIds(String includedModuleIds) {
		this.includedModuleIds = includedModuleIds;
	}

	public boolean isEnableMRCMValidation() {
		return enableMRCMValidation;
	}

	public void setEnableMRCMValidation(boolean enableMRCMValidation) {
		this.enableMRCMValidation = enableMRCMValidation;
	}

	public void setContentHeadTimestamp(Long contentHeadTimestamp) {
		this.contentHeadTimestamp = contentHeadTimestamp;
	}

	public Long getContentHeadTimestamp() {
		return contentHeadTimestamp;
	}
}
