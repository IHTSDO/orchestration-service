package org.ihtsdo.ts.importer;

import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.ts.importer.clients.WorkbenchWorkflowClient;
import org.ihtsdo.ts.importer.clients.WorkbenchWorkflowClientException;
import org.ihtsdo.ts.importer.clients.jira.JiraDataHelper;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Importer {

	public static final String SELECTED_ARCHIVE_VERSION = "SelectedArchiveVersion";
	@Autowired
	private WorkbenchWorkflowClient workbenchWorkflowClient;

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraProjectSync jiraContentProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	@Autowired
	private JiraDataHelper jiraDataHelper;

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ImportResult importSelectedWBContent() throws ImporterException {
		logger.info("Started");
		Date startDate = new Date();

		ImportResult importResult = new ImportResult();

		try {
			// Create Jira issue
			String taskKey = jiraContentProjectSync.createTask(SIMPLE_DATE_FORMAT.format(startDate));
			importResult.setTaskKey(taskKey);

			try {
				List<Long> completedConceptIds = workbenchWorkflowClient.getCompletedConceptSctids();

				// Create selection archive (automatically pulls in any new daily exports first)
				SelectionResult selectionResult = importFilterService.createSelectionArchive(completedConceptIds.toArray(new Long[]{}));

				if (selectionResult.isSuccess()) {
					// Create TS branch
					tsClient.getCreateBranch(taskKey);

					// Stream selection archive into TS import process
					String selectedArchiveVersion = selectionResult.getSelectedArchiveVersion();
					jiraDataHelper.putData(jiraContentProjectSync.findIssue(taskKey), SELECTED_ARCHIVE_VERSION, selectedArchiveVersion);
					logger.info("Filter version {}", selectedArchiveVersion);
					InputStream selectionArchiveStream = importFilterService.getSelectionArchive(selectedArchiveVersion);
					boolean importSuccessful = tsClient.importRF2Archive(taskKey, selectionArchiveStream);
					if (importSuccessful) {
						importResult.setImportCompletedSuccessfully(true);
						jiraContentProjectSync.addComment(taskKey, "Created task with selection from workbench daily export. SCTID list: " + toString(selectionResult.getFoundConceptIds()));
						jiraContentProjectSync.updateStatus(taskKey, JiraTransitions.IMPORT_CONTENT);
					} else {
						handleError("Import process failed, see SnowOwl logs for details.", importResult);
					}

					return importResult;
				} else {
					if (selectionResult.isMissingDependencies()) {
						return handleError("The concept selection should be extended to include the following dependencies: " + selectionResult.getMissingDependencies(), importResult);
					} else if (selectionResult.isEmptySelection()) {
						return handleError("The current selection did not match anything in the backlog.", importResult);
					} else {
						return handleError("Unknown selection problem.", importResult);
					}
				}
			} catch (ImportFilterServiceException e) {
				return handleError("Error during selection archive creation process.", importResult, e);
			} catch (SnowOwlRestClientException e) {
				return handleError("Error using Snow Owl Terminology Server.", importResult, e);
			} catch (WorkbenchWorkflowClientException e) {
				return handleError(e.getMessage(), importResult, e);
			}
		} catch (JiraException e) {
			throw new ImporterException("Error using Jira.", e);
		}
	}

	private ImportResult handleError(String message, ImportResult importResult, Exception e) {
		logger.error(message, e);
		return handleError(message, importResult);
	}
	private ImportResult handleError(String message, ImportResult importResult) {
		try {
			jiraContentProjectSync.addComment(importResult.getTaskKey(), "Error: " + message);
		} catch (JiraException e) {
			logger.error("Failed to add error comment to issue.", e);
		}
		return importResult.setMessage(message);
	}

	private String toString(Set<Long> sctids) {
		StringBuilder builder = new StringBuilder();
		for (Long sctid : sctids) {
			if (builder.length() > 0) builder.append(",");
			builder.append("|");
			builder.append(sctid.toString());
			builder.append("|");
		}
		return builder.toString();
	}

}
