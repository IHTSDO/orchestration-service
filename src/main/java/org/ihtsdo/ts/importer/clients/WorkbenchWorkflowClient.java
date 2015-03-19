package org.ihtsdo.ts.importer.clients;

import org.ihtsdo.ts.importfilter.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

public class WorkbenchWorkflowClient {

	@Autowired
	private ArchiveService archiveService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public List<Long> getCompletedConceptSctids() throws WorkbenchWorkflowClientException {
		try {
			List<Long> latestCompletedConceptIds = archiveService.getLatestCompletedConceptIds();
			logger.info("{} completed concepts found in workflow history.", latestCompletedConceptIds.size());
			return latestCompletedConceptIds;
		} catch (IOException e) {
			throw new WorkbenchWorkflowClientException("Failed to read list of completed concept ids.");
		}
	}

}
