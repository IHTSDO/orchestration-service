package org.ihtsdo.orchestration.clients.workbenchdata;

public class WorkbenchWorkflowClientException extends Exception {

	public WorkbenchWorkflowClientException(String message) {
		super(message);
	}

	public WorkbenchWorkflowClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
