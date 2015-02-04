package org.ihtsdo.ts.importer;

import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.ts.importer.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.jira.JiraSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Importer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private S3Client s3Client;

	@Autowired
	private JiraProjectSync jiraProjectSync;

	public void go() {
		logger.info("Started");
		try {
			jiraProjectSync.assertProjectExists();
		} catch (JiraSyncException e) {
			logger.error("Sync Failed", e);
		}
		logger.info("Finished");
	}

}
