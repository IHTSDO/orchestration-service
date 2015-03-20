package org.ihtsdo.ts.importer;

import java.io.File;

import org.ihtsdo.srs.client.SRSRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
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

//		applicationContext.getBean(SnowOwlRestClient.class).classify("test");

		applicationContext.getBean(SnowOwlRestClient.class).exportBranch("pgw_test2", SnowOwlRestClient.EXTRACT_TYPE.DELTA);

		// String testInputFilesDir = "/Users/Peter/git/srs-script-client/ts_daily_build/tmp/srs_input_files";
		// applicationContext.getBean(SRSRestClient.class).runDailyBuild(new File(testInputFilesDir), "20150319");
	}

}
