package org.ihtsdo.ts.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class RestApplication {

	private static Logger LOGGER = LoggerFactory.getLogger(RestApplication.class);

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Config.class, "server.port=9000");

		// Uncomment next line to trigger import on startup (useful for testing)
//		applicationContext.getBean(ImporterService.class).importCompletedWBContent();
	}

}
