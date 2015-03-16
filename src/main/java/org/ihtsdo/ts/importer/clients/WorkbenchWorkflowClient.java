package org.ihtsdo.ts.importer.clients;

import com.amazonaws.services.s3.model.S3Object;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class WorkbenchWorkflowClient {

	public static final String COMPLETED_CONCEPT_IDS_TXT = "completed_concept_ids.txt";

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String filterBucket;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Set<Long> getCompletedConceptSctids() throws WorkbenchWorkflowClientException {
		Set<Long> set = new HashSet<>();

		S3Object completedConceptIdFile = s3Client.getObject(filterBucket, COMPLETED_CONCEPT_IDS_TXT);
		if (completedConceptIdFile != null) {
			try (BufferedReader sctidReader = new BufferedReader(new InputStreamReader(completedConceptIdFile.getObjectContent()))) {
				String line;
				while ((line = sctidReader.readLine()) != null) {
					set.add(Long.parseLong(line));
				}
			} catch (IOException e) {
				throw new WorkbenchWorkflowClientException("Failed to read list of completed concept ids.");
			}
		} else {
			throw new WorkbenchWorkflowClientException("List of completed concept ids not found.");
		}
		logger.info("Found {} concept ids in concept complete list.", set.size());
		return set;
	}

}
