package org.ihtsdo.orchestration.importer;

import org.ihtsdo.orchestration.clients.snowowl.ImportError;
import org.ihtsdo.orchestration.clients.snowowl.ImportErrorParser;
import org.ihtsdo.orchestration.clients.snowowl.ImportErrorParserException;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportBlacklistService {

	@Autowired
	private ImportErrorParser importErrorParser;

	@Autowired
	private SnowOwlRestClient tsClient;

	public ImportBlacklistResults createBlacklistFromLatestImportErrors() throws ImportBlacklistServiceException {
		try {
			List<ImportError> importErrors = importErrorParser.parseLogForLatestImportErrors(tsClient.getRolloverLogStream(), tsClient.getLogStream());
			List<Long> blacklistedConcepts = new ArrayList<>();
			for (ImportError importError : importErrors) {
				blacklistedConcepts.add(Long.parseLong(importError.getConceptId()));
			}
			return new ImportBlacklistResults(importErrors, blacklistedConcepts);
		} catch (IOException | ImportErrorParserException e) {
			throw new ImportBlacklistServiceException("Failed to parse import error log.", e);
		}
	}
}
