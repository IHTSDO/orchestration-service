package org.ihtsdo.ts.importer;

import org.ihtsdo.otf.dao.s3.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

public class Application {

	private static Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		ApplicationContext applicationContext = createApplicationContext();

		// TODO: Switch to online S3 Client when finished testing
		// Prime offline bucket
		S3Client s3Client = applicationContext.getBean(S3Client.class);
		String archiveBucket = applicationContext.getBean("archiveBucket", String.class);
		File dir = new File("/Users/kaikewley/leg/");
		File[] files = dir.listFiles();
		for (File file : files) {
			s3Client.putObject(archiveBucket, file.getName(), file);
		}

		// Run importer
		Importer importer = applicationContext.getBean(Importer.class);
		try {
			ImportResult importResult = importer.importSelectedWBContent("task_test", new Long[]{10641351000119109L});
			if (importResult.getCompletedSuccessfully()) {
				LOGGER.info("Completed successfully");
			} else {
				LOGGER.info("Failure - " + importResult.getMessage());
			}
		} catch (ImporterException e) {
			LOGGER.error("Unrecoverable error", e);
		}
	}

	private static ApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(new String[] {"spring_context.xml"});
	}

}
