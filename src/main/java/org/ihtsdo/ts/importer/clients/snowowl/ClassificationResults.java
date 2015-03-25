package org.ihtsdo.ts.importer.clients.snowowl;

public class ClassificationResults {
	private boolean equivalentConceptsFound;
	private boolean relationshipChangesFound;
	private String classificationId;

	public void setEquivalentConceptsFound(boolean equivalentConceptsFound) {
		this.equivalentConceptsFound = equivalentConceptsFound;
	}

	public boolean isEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}

	public void setRelationshipChangesFound(boolean relationshipChangesFound) {
		this.relationshipChangesFound = relationshipChangesFound;
	}

	public boolean isRelationshipChangesFound() {
		return relationshipChangesFound;
	}

	public void setClassificationId(String classificationId) {
		this.classificationId = classificationId;
	}

	public String getClassificationId() {
		return classificationId;
	}
}
