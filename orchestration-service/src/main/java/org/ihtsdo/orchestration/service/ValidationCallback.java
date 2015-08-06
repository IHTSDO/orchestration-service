package org.ihtsdo.orchestration.service;

public interface ValidationCallback {
	void complete(ValidationService.ValidationStatus finalValidationStatus);
}
