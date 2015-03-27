package org.ihtsdo.ts.importer;

import org.ihtsdo.ts.importer.clients.snowowl.ImportError;
import org.ihtsdo.ts.importer.clients.snowowl.ImportErrorParser;
import org.ihtsdo.ts.importer.clients.snowowl.ImportErrorParserException;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportBlacklistService {

	@Autowired
	private ImportErrorParser importErrorParser;

	@Autowired
	private SnowOwlRestClient tsClient;

	public List<Long> createBlacklistFromLatestImportErrors() throws ImportBlacklistServiceException {
		try {
			List<ImportError> importErrors = importErrorParser.parseLogForLatestImportErrors(tsClient.getRolloverLogStream(), tsClient.getLogStream());
			List<Long> blacklistedConcepts = new ArrayList<>();
			for (ImportError importError : importErrors) {
				blacklistedConcepts.add(Long.parseLong(importError.getConceptId()));
			}
			return blacklistedConcepts;
		} catch (IOException | ImportErrorParserException e) {
			throw new ImportBlacklistServiceException("Failed to parse import error log.", e);
		}
	}
}
