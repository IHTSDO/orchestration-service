package org.ihtsdo.ts.importer;

import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
import org.ihtsdo.ts.importfilter.ImportFilterService;
import org.ihtsdo.ts.importfilter.ImportFilterServiceException;
import org.ihtsdo.ts.importfilter.SelectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;

public class Importer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraProjectSync jiraContentProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	public ImportResult importSelectedWBContent(String taskLabel, Long[] sctids) throws ImporterException {
		logger.info("Started");
		ImportResult importResult = new ImportResult();

		try {
			// Create selection archive (automatically pulls in any new daily exports first)
			SelectionResult selectionResult = importFilterService.createSelectionArchive(sctids);

			if (selectionResult.isSuccess()) {
				// Create TS branch
				boolean newBranch = tsClient.getCreateBranch(taskLabel);

				// Stream selection archive into TS import process
				logger.info("Filter version {}", selectionResult.getFilteredArchiveVersion());
				InputStream selectionArchiveStream = importFilterService.getSelectionArchive(selectionResult.getFilteredArchiveVersion());
				boolean importSuccessful = tsClient.importRF2(taskLabel, selectionArchiveStream);
				if (importSuccessful) {
					importResult.setImportCompletedSuccessfully(true);
				} else {
					return importResult.setMessage("Import process failed, see SnowOwl logs for details.");
				}

				// TODO: Using Jira issue label seems risky. Maybe use a custom field to identify tasks?
				boolean taskExists = jiraContentProjectSync.doesTaskExist(taskLabel);
				if (!taskExists) {
					jiraContentProjectSync.createTask(taskLabel);
				}
				String operator = newBranch ? "Created" : "Updated";
				jiraContentProjectSync.addComment(taskLabel, operator + " task with selection from workbench daily export. SCTID list: " + toString(sctids));

				return importResult;
			} else {
				if (selectionResult.isMissingDependencies()) {
					return importResult.setMessage("The concept selection should be extended to include the following dependencies: " + selectionResult.getMissingDependencies());
				} else if (selectionResult.isEmptySelection()) {
					return importResult.setMessage("The current selection did not match anything in the backlog.");
				} else {
					return importResult.setMessage("Unknown selection problem.");
				}
			}
		} catch (ImportFilterServiceException e) {
			throw new ImporterException("Error during selection archive creation process.", e);
		} catch (SnowOwlRestClientException e) {
			throw new ImporterException("Error using Snow Owl Terminology Server.", e);
		} catch (JiraException e) {
			throw new ImporterException("Error using Jira.", e);
		}
	}

	private String toString(Long[] sctids) {
		StringBuilder builder = new StringBuilder();
		for (Long sctid : sctids) {
			if (builder.length() > 0) builder.append(",");
			builder.append(sctid.toString());
		}
		return builder.toString();
	}

}
