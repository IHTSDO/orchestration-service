package org.ihtsdo.ts.importer.clients.snowowl;

public class ImportError {

	private final String conceptId;
	private final String message;

	public ImportError(String conceptId, String message) {
		this.conceptId = conceptId;
		this.message = message;
	}

	public String getConceptId() {
		return conceptId;
	}

	public String getMessage() {
		return message;
	}

}
