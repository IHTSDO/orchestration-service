package org.ihtsdo.orchestration.clients.rvf;


public class ValidationConfiguration {

	private String previousInternationalRelease;
	private String assertionGroupNames;
	private String failureExportMax;
	private String previousExtensionRelease;
	private String exentsionDependencyRelease;
	private String productName;
	
	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	private String releaseDate;
	
	public String getExentsionDependencyRelease() {
		return exentsionDependencyRelease;
	}

	public String getPreviousExtensionRelease() {
		return previousExtensionRelease;
	}

	@Override
	public ValidationConfiguration clone()  {
		ValidationConfiguration config = new ValidationConfiguration();
		config.setAssertionGroupNames(this.assertionGroupNames);
		config.setExentsionDependencyRelease(this.exentsionDependencyRelease);
		config.setFailureExportMax(this.failureExportMax);
		config.setPreviousExtensionRelease(this.previousExtensionRelease);
		config.setProductName(this.productName);
		config.setReleaseDate(this.releaseDate);
		config.setPreviousInternationalRelease(this.previousInternationalRelease);
		return config;
	
	}
	
	public String getPreviousInternationalRelease() {
		return previousInternationalRelease;
	}

	public void setPreviousInternationalRelease(String previousInternationalRelease) {
		this.previousInternationalRelease = previousInternationalRelease;
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

	public void setPreviousExtensionRelease(String previousExtension) {
		this.previousExtensionRelease = previousExtension;
		
	}

	public void setExentsionDependencyRelease(String extensionDependencyRelease) {
		this.exentsionDependencyRelease = extensionDependencyRelease;
	}

	public String getProductName() {
		return this.productName;
	}

	public String getReleaseDate() {
		return this.releaseDate;
	}
}
