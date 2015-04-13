package org.ihtsdo.orchestration.importer;

public class ImportBlacklistServiceException extends Exception {

	public ImportBlacklistServiceException(String message) {
		super(message);
	}

	public ImportBlacklistServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
