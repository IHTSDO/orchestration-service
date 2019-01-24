package org.ihtsdo.orchestration.clients.rvf;

public class ValidationConfiguration {

	private String previousRelease;
	private String assertionGroupNames;
	private String failureExportMax = "10";  //Default value if not otherwise specified.
	private String dependencyRelease;
	private String productName;
	private String releaseDate;
	private String releaseCenter;
	
	
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
		if (!msgBuilder.toString().isEmpty()) {
			return msgBuilder.toString();
		}
		return null;
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
				+ assertionGroupNames + ", failureExportMax="
				+ failureExportMax + ", previousRelease="
				+ previousRelease + ", dependencyRelease="
				+ dependencyRelease + ",releaseCenter="
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
				+ ((dependencyRelease == null) ? 0
						: dependencyRelease.hashCode());
		result = prime
				* result
				+ ((failureExportMax == null) ? 0 : failureExportMax.hashCode());
		result = prime
				* result
				+ ((previousRelease == null) ? 0
						: previousRelease.hashCode());
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
		if (dependencyRelease == null) {
			if (other.dependencyRelease != null)
				return false;
		} else if (!dependencyRelease
				.equals(other.dependencyRelease))
			return false;
		if (failureExportMax == null) {
			if (other.failureExportMax != null)
				return false;
		} else if (!failureExportMax.equals(other.failureExportMax))
			return false;
		if (previousRelease == null) {
			if (other.previousRelease != null)
				return false;
		} else if (!previousRelease
				.equals(other.previousRelease))
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
