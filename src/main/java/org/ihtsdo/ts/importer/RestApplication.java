package org.ihtsdo.ts.importer;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class RestApplication {

	// private static Logger LOGGER = LoggerFactory.getLogger(RestApplication.class);

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Config.class, "server.port=9000");

		// Uncomment next line to trigger import on startup (useful for testing)

		// applicationContext.getBean(JiraProjectSync.class).createTask("Testing Priority");

//		applicationContext.getBean(ImporterService.class).importCompletedWBContent(null);

//		applicationContext.getBean(SnowOwlRestClient.class).classify("test");

//		applicationContext.getBean(SnowOwlRestClient.class).exportBranch("pgw_test2", SnowOwlRestClient.EXTRACT_TYPE.DELTA);

//		String testInputFilesDir = "/Users/Peter/git/srs-script-client/ts_daily_build/tmp/srs_input_files";
//		applicationContext.getBean(SRSRestClient.class).runDailyBuild(new File(testInputFilesDir), "20150319");

//		applicationContext.getBean(SnowOwlRestClient.class).promoteBranch("pgw_test2");

	}

}
