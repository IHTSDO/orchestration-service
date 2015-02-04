package org.ihtsdo.ts.importer;

import org.ihtsdo.ts.importer.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.jira.JiraSyncException;
import org.ihtsdo.ts.importer.s3.S3SyncClient;
import org.ihtsdo.ts.importer.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.snowowl.SnowOwlRestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class Importer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private S3SyncClient s3SyncClient;

	@Autowired
	private JiraProjectSync jiraProjectSync;

	@Autowired
	private SnowOwlRestClient snowOwlRestClient;

	public void go() {
		logger.info("Started");
		try {
			// Test S3 connection
			List<String> filenames = s3SyncClient.listFiles();
			System.out.println("S3 Objects:");
			for (String filename : filenames) {
				System.out.println(" " + filename);
			}

			// Test Jira connection
			jiraProjectSync.assertProjectExists();

			// Test Snow Owl connection
			List<String> branchNames = snowOwlRestClient.listBranches();
			System.out.println("Snow Owl Branches:");
			for (String branchName : branchNames) {
				System.out.println(" " + branchName);
			}
		} catch (JiraSyncException | SnowOwlRestClientException e) {
			logger.error("Sync Failed", e);
		}
		logger.info("Finished");
	}

}
