package org.ihtsdo.ts.importer;

import org.ihtsdo.ts.importer.clients.snowowl.ImportError;

import java.util.List;

public class ImportBlacklistResults {

	private final List<ImportError> importErrors;
	private final List<Long> blacklistedConcepts;

	public ImportBlacklistResults(List<ImportError> importErrors, List<Long> blacklistedConcepts) {
		this.importErrors = importErrors;
		this.blacklistedConcepts = blacklistedConcepts;
	}

	public List<ImportError> getImportErrors() {
		return importErrors;
	}

	public List<Long> getBlacklistedConcepts() {
		return blacklistedConcepts;
	}

}
