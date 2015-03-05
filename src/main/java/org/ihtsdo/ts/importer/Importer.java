package org.ihtsdo.ts.importer;

import org.ihtsdo.ts.importer.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.snowowl.SnowOwlRestClientException;
import org.ihtsdo.ts.importfilter.ImportFilterService;
import org.ihtsdo.ts.importfilter.ImportFilterServiceException;
import org.ihtsdo.ts.importfilter.SelectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.InputStream;

public class Importer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraProjectSync jiraImportProjectSync;

	@Autowired
	private JiraProjectSync jiraContentProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	public ImportResult importSelectedWBContent(String taskLabel, Long[] sctids) throws ImporterException {
		logger.info("Started");
		ImportResult importResult = new ImportResult();

		try {
			SelectionResult selectionResult = importFilterService.createSelectionArchive(sctids);

			if (selectionResult.isSuccess()) {
				tsClient.getCreateBranch(taskLabel);

				String filteredArchiveVersion = selectionResult.getFilteredArchiveVersion();
				logger.info("Filter version {}", filteredArchiveVersion);
				InputStream selectionArchiveStream = importFilterService.getSelectionArchive(filteredArchiveVersion);
				Assert.notNull(selectionArchiveStream, "Archive to import should not be null.");
				tsClient.importRF2(taskLabel, selectionArchiveStream);
				return importResult.success();
			} else {
				if (selectionResult.isMissingDependencies()) {
					return importResult.fail("The concept selection should be extended to include the following dependencies: " + selectionResult.getMissingDependencies());
				} else if (selectionResult.isEmptySelection()) {
					return importResult.fail("The current selection did not match anything in the backlog.");
				} else {
					return importResult.fail("Unknown selection problem.");
				}
			}
		} catch (ImportFilterServiceException e) {
			throw new ImporterException("Error during selection archive creation process.", e);
		} catch (SnowOwlRestClientException e) {
			throw new ImporterException("Error using Snow Owl Terminology Server.", e);
		}
	}

}
