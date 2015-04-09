package org.ihtsdo.orchestration.importer;

import org.ihtsdo.ts.importfilter.SelectionResult;

public class ImportResult {

	private boolean importCompletedSuccessfully;
	private String message;
	private String taskKey;
	private SelectionResult selectionResult;
	private boolean buildingBlacklistFailed;

	public ImportResult() {
	}

	public ImportResult(String taskKey) {
		this.taskKey = taskKey;
	}

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

	public void setSelectionResult(SelectionResult selectionResult) {
		this.selectionResult = selectionResult;
	}

	public SelectionResult getSelectionResult() {
		return selectionResult;
	}

	public void setBuildingBlacklistFailed(boolean buildingBlacklistFailed) {
		this.buildingBlacklistFailed = buildingBlacklistFailed;
	}

	public boolean isBuildingBlacklistFailed() {
		return buildingBlacklistFailed;
	}
}
