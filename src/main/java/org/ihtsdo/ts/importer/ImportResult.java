package org.ihtsdo.ts.importer;

public class ImportResult {

	private boolean importCompletedSuccessfully;
	private String message;
	private String taskKey;

	public void setImportCompletedSuccessfully(boolean importCompletedSuccessfully) {
		this.importCompletedSuccessfully = importCompletedSuccessfully;
	}

	public ImportResult setMessage(String message) {
		this.message = message;
		return this;
	}

	public boolean isImportCompletedSuccessfully() {
		return importCompletedSuccessfully;
	}

	public String getMessage() {
		return message;
	}

	public void setTaskKey(String taskKey) {
		this.taskKey = taskKey;
	}

	public String getTaskKey() {
		return taskKey;
	}
}
