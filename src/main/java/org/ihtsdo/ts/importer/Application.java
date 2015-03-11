package org.ihtsdo.ts.importer;

import org.ihtsdo.otf.dao.s3.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Application {

	@Autowired
	private Importer importer;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String archiveBucket;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public static void main_manual_testing(String[] args) {
		String task_test = "task_test";
		Long[] sctidsToSelect = {84625002L};

		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[]{"spring_context.xml"});
		Application application = applicationContext.getBean(Application.class);
		application.importSelectedContent(task_test, sctidsToSelect);
	}

	private void importSelectedContent(String taskLabel, Long[] sctidsToSelect) {
		// Run importer
		try {
			ImportResult importResult = importer.importSelectedWBContent(taskLabel, sctidsToSelect);
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
