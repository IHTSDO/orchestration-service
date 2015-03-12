package org.ihtsdo.ts.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ImporterService {

	@Autowired
	private Importer importer;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public void importCompletedWBContent() {
		// Run importer
		try {
			ImportResult importResult = importer.importSelectedWBContent();
			if (importResult.isImportCompletedSuccessfully()) {
				logger.info("Completed successfully");
			} else {
				logger.info("Failure - " + importResult.getMessage());
			}
		} catch (ImporterException e) {
			logger.error("Unrecoverable error", e);
		}

	}

}
