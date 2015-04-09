package org.ihtsdo.orchestration.clients.jira;

public class JiraSyncException extends Exception {

	public JiraSyncException(String message) {
		super(message);
	}

	public JiraSyncException(String message, Throwable cause) {
		super(message, cause);
	}
}
