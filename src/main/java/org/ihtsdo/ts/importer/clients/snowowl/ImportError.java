package org.ihtsdo.ts.importer.clients.snowowl;

public class ImportError {

	private final ImportErrorType type;
	private final String conceptId;

	public ImportError(ImportErrorType type, String conceptId) {
		this.type = type;
		this.conceptId = conceptId;
	}

	public ImportErrorType getType() {
		return type;
	}

	public String getConceptId() {
		return conceptId;
	}

	public enum ImportErrorType {
		DESCRIPTION_CONCEPT_DOES_NOT_EXIST, RELATIONSHIP_DESTINATION_CONCEPT_DOES_NOT_EXIST, CONCEPT_INACTIVATION_WHEN_REFERENCED_IN_ACTIVE_RELATIONSHIPS, RELATIONSHIP_SOURCE_CONCEPT_DOES_NOT_EXIST, DESCRIPTION_IDENTIFIER_NOT_UNIQUE
	}

}
