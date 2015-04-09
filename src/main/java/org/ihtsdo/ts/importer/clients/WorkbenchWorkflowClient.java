package org.ihtsdo.ts.importer.clients;

import org.ihtsdo.ts.importfilter.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorkbenchWorkflowClient {

	@Autowired
	private ArchiveService archiveService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public Map<Long, Boolean> getConceptIdsWithApprovedStatus() throws WorkbenchWorkflowClientException {
		try {
			Map<Long, Boolean> latestCompletedConceptIds = archiveService.getLatestConceptIdsWithApprovedStatus();
			logger.info("{} concepts found in workflow history.", latestCompletedConceptIds.size());
			return latestCompletedConceptIds;
		} catch (IOException e) {
			throw new WorkbenchWorkflowClientException("Failed to read list of concepts.");
		}
	}

	public Set<Long> getIncompleteConceptIds(Map<Long, Boolean> conceptIdsWithApprovedStatus) {
		return getConceptIdsOfStatus(conceptIdsWithApprovedStatus, false);
	}

	public Set<Long> getCompleteConceptIds(Map<Long, Boolean> conceptIdsWithApprovedStatus) {
		return getConceptIdsOfStatus(conceptIdsWithApprovedStatus, true);
	}

	public Set<Long> getConceptIdsOfStatus(Map<Long, Boolean> conceptIdsWithApprovedStatus, Boolean status) {
		Set<Long> withStatus = new HashSet<>();
		for (Long conceptId : conceptIdsWithApprovedStatus.keySet()) {
			if (conceptIdsWithApprovedStatus.get(conceptId).equals(status)) {
				withStatus.add(conceptId);
			}
		}
		return withStatus;
	}
}
