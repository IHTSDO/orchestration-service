package org.ihtsdo.orchestration.schedule;

import org.ihtsdo.orchestration.importer.ImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class StartFilteredImport implements Runnable {

	@Autowired
	private ImporterService importerService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void run() {
		logger.info("Scheduled import triggered");
		importerService.importCompletedWBContentAsync(null, false);
	}

}