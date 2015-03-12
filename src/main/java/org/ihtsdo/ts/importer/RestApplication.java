package org.ihtsdo.ts.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class RestApplication {

	private static Logger LOGGER = LoggerFactory.getLogger(RestApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Config.class, "server.port=9000");
		Importer importer = applicationContext.getBean(Importer.class);
		importSelectedContent(importer);
	}

	private static void importSelectedContent(Importer importer) {
		// Run importer
		try {
			ImportResult importResult = importer.importSelectedWBContent();
			if (importResult.isImportCompletedSuccessfully()) {
				LOGGER.info("Completed successfully");
			} else {
				LOGGER.info("Failure - " + importResult.getMessage());
			}
		} catch (ImporterException e) {
			LOGGER.error("Unrecoverable error", e);
		}

	}

}
