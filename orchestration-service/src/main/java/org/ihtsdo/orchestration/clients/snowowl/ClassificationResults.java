package org.ihtsdo.orchestration.clients.snowowl;

import java.io.File;

public class ClassificationResults {
	private boolean equivalentConceptsFound;
	private int relationshipChangesCount;
	private String classificationId;
	private String equivalentConceptsJson;
	private File relationshipChangesFile;

	public void setEquivalentConceptsFound(boolean equivalentConceptsFound) {
		this.equivalentConceptsFound = equivalentConceptsFound;
	}

	public boolean isEquivalentConceptsFound() {
		return equivalentConceptsFound;
	}

	public void setRelationshipChangesCount(int relationshipChangesCount) {
		this.relationshipChangesCount = relationshipChangesCount;
	}

	public int getRelationshipChangesCount() {
		return relationshipChangesCount;
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

	public void setRelationshipChangesFile(File relationshipChangesFile) {
		this.relationshipChangesFile = relationshipChangesFile;
	}

	public File getRelationshipChangesFile() {
		return relationshipChangesFile;
	}

	public boolean isRelationshipChangesFound() {
		return relationshipChangesCount > 0;
	}
}
