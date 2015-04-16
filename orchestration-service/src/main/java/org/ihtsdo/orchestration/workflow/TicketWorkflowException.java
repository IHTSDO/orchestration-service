package org.ihtsdo.orchestration.workflow;

public class TicketWorkflowException extends Exception {

	public TicketWorkflowException(String message) {
		super(message);
	}

	public TicketWorkflowException(String message, Throwable cause) {
		super(message, cause);
	}
}
