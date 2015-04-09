package org.ihtsdo.orchestration.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImporterService {

	@Autowired
	private Importer importer;

	private ExecutorService executorService;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public ImporterService() {
		executorService = Executors.newSingleThreadExecutor();
	}

	public void importCompletedWBContent(Set<Long> selectConceptIdsOverride) {
		// Run importer
		try {
			ImportResult importResult = importer.importSelectedWBContent(selectConceptIdsOverride);
			if (importResult.isImportCompletedSuccessfully()) {
				logger.info("Completed successfully");
			} else {
				logger.info("Failure - " + importResult.getMessage());
			}
		} catch (ImporterException e) {
			logger.error("Unrecoverable error", e);
		}

	}

	public void importCompletedWBContentAsync(final Set<Long> selectConceptIdsOverride) {
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				importCompletedWBContent(selectConceptIdsOverride);
			}
		});
	}
}
