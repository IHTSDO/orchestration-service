package org.ihtsdo.orchestration.clients.snowowl;

public class ImportErrorParserException extends Exception {

	public ImportErrorParserException(String message) {
		super(message);
	}

	public ImportErrorParserException(String message, Throwable cause) {
		super(message, cause);
	}
}
