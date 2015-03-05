package org.ihtsdo.ts.importer;

public class ImportResult {

	private Boolean completedSuccessfully;
	private String message;

	public ImportResult fail(String message) {
		completedSuccessfully = false;
		this.message = message;
		return this;
	}

	public ImportResult success() {
		completedSuccessfully = true;
		return this;
	}

	public Boolean getCompletedSuccessfully() {
		return completedSuccessfully;
	}

	public String getMessage() {
		return message;
	}
}
