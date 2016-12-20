package org.ihtsdo.orchestration.clients.rvf;



public class ValidationConfiguration {

	private String previousInternationalRelease;
	private String assertionGroupNames;
	private String failureExportMax = "10";  //Default value if not otherwise specified.
	private String previousExtensionRelease;
	private String extensionDependencyRelease;
	private String productName;
	private String releaseDate;
	private String releaseCenter;
	
	
	public String checkMissingParameters() {
		StringBuilder msgBuilder = new StringBuilder();
		if (this.assertionGroupNames == null) {
			msgBuilder.append("assertionGroupNames can't be null.");
		}
		if (extensionDependencyRelease == null && previousInternationalRelease == null && previousExtensionRelease == null) {
			msgBuilder.append("previousInternationalRelease,extensionDependencyRelease and previousExtensionRelease can't be all null.");
		} else {
			if (previousInternationalRelease == null) {
				if (extensionDependencyRelease ==null || previousExtensionRelease == null) {
					msgBuilder.append("previousExtensionRelease and extensionDependencyRelease can't be null for extension validation.");
				}
			}
		}
		if (!msgBuilder.toString().isEmpty()) {
			return msgBuilder.toString();
		}
		return null;
	}
	
	
	public String getExtensionDependencyRelease() {
		return extensionDependencyRelease;
	}

	public String getPreviousExtensionRelease() {
		return previousExtensionRelease;
	}
	
	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
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

	public void setExtensionDependencyRelease(String extensionDependencyRelease) {
		this.extensionDependencyRelease = extensionDependencyRelease;
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
				+ assertionGroupNames + ", failureExportMax="
				+ failureExportMax + ", previousInternationalRelease="
				+ previousInternationalRelease + ", previousExtensionRelease="
				+ previousExtensionRelease + ", exentsionDependencyRelease="
				+ extensionDependencyRelease + ",releaseCenter="
				+ releaseCenter + "]";
	}	

	public void setReleaseCenter(String releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public String getReleaseCenter() {
		return this.releaseCenter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((assertionGroupNames == null) ? 0 : assertionGroupNames
						.hashCode());
		result = prime
				* result
				+ ((extensionDependencyRelease == null) ? 0
						: extensionDependencyRelease.hashCode());
		result = prime
				* result
				+ ((failureExportMax == null) ? 0 : failureExportMax.hashCode());
		result = prime
				* result
				+ ((previousExtensionRelease == null) ? 0
						: previousExtensionRelease.hashCode());
		result = prime
				* result
				+ ((previousInternationalRelease == null) ? 0
						: previousInternationalRelease.hashCode());
		result = prime * result
				+ ((productName == null) ? 0 : productName.hashCode());
		result = prime * result
				+ ((releaseCenter == null) ? 0 : releaseCenter.hashCode());
		result = prime * result
				+ ((releaseDate == null) ? 0 : releaseDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidationConfiguration other = (ValidationConfiguration) obj;
		if (assertionGroupNames == null) {
			if (other.assertionGroupNames != null)
				return false;
		} else if (!assertionGroupNames.equals(other.assertionGroupNames))
			return false;
		if (extensionDependencyRelease == null) {
			if (other.extensionDependencyRelease != null)
				return false;
		} else if (!extensionDependencyRelease
				.equals(other.extensionDependencyRelease))
			return false;
		if (failureExportMax == null) {
			if (other.failureExportMax != null)
				return false;
		} else if (!failureExportMax.equals(other.failureExportMax))
			return false;
		if (previousExtensionRelease == null) {
			if (other.previousExtensionRelease != null)
				return false;
		} else if (!previousExtensionRelease
				.equals(other.previousExtensionRelease))
			return false;
		if (previousInternationalRelease == null) {
			if (other.previousInternationalRelease != null)
				return false;
		} else if (!previousInternationalRelease
				.equals(other.previousInternationalRelease))
			return false;
		if (productName == null) {
			if (other.productName != null)
				return false;
		} else if (!productName.equals(other.productName))
			return false;
		if (releaseCenter == null) {
			if (other.releaseCenter != null)
				return false;
		} else if (!releaseCenter.equals(other.releaseCenter))
			return false;
		if (releaseDate == null) {
			if (other.releaseDate != null)
				return false;
		} else if (!releaseDate.equals(other.releaseDate))
			return false;
		return true;
	}	
}
