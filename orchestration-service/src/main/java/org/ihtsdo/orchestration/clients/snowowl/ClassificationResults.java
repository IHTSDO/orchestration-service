package org.ihtsdo.orchestration.clients.snowowl;

public class ClassificationResults {
	private boolean equivalentConceptsFound;
	private boolean relationshipChangesFound;
	private String classificationId;
	private String equivalentConceptsJson;
	private String relationshipChangesJson;

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

	public void setEquivalentConceptsJson(String equivalentConceptsJson) {
		this.equivalentConceptsJson = equivalentConceptsJson;
	}

	public String getEquivalentConceptsJson() {
		return equivalentConceptsJson;
	}

	public void setRelationshipChangesJson(String relationshipChangesJson) {
		this.relationshipChangesJson = relationshipChangesJson;
	}

	public String getRelationshipChangesJson() {
		return relationshipChangesJson;
	}
}
