package org.ihtsdo.ts.importer;

import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.ts.importer.clients.WorkbenchWorkflowClient;
import org.ihtsdo.ts.importer.clients.WorkbenchWorkflowClientException;
import org.ihtsdo.ts.importer.clients.googledocs.GooglePublishedSheetsClient;
import org.ihtsdo.ts.importer.clients.googledocs.GooglePublishedSheetsClientException;
import org.ihtsdo.ts.importer.clients.jira.JiraDataHelper;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
import org.ihtsdo.ts.importfilter.*;
import org.ihtsdo.ts.workflow.DailyDeltaTicketWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Importer {

	public static final String SELECTED_ARCHIVE_VERSION = "SelectedArchiveVersion";

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	@Autowired
	private WorkbenchWorkflowClient workbenchWorkflowClient;

	@Autowired
	private GooglePublishedSheetsClient sheetsClient;

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraProjectSync jiraContentProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	@Autowired
	private JiraDataHelper jiraDataHelper;

	@Autowired
	private BacklogContentService backlogContentService;

	@Autowired
	private ImportFilter importFilter;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ImportResult importSelectedWBContent(Set<Long> selectConceptIdsOverride) throws ImporterException {
		logger.info("Started");
		Date startDate = new Date();

		ImportResult importResult = new ImportResult();

		try {
			// Create Jira issue
			String taskKey = jiraContentProjectSync.createTask(SIMPLE_DATE_FORMAT.format(startDate));
			importResult.setTaskKey(taskKey);

			try {
				importFilterService.importNewWorkbenchArchives();

				Set<Long> completedConceptIds;
				if (selectConceptIdsOverride != null) {
					completedConceptIds = selectConceptIdsOverride;
					jiraContentProjectSync.addComment(taskKey, "Concept selection override, SCTID list: " + toJiraSearchableIdList(selectConceptIdsOverride));
				} else {
					completedConceptIds = new HashSet<>(workbenchWorkflowClient.getCompletedConceptSctids());
					List<String> conceptBlacklistStrings = sheetsClient.getColumnValues();
					if (conceptBlacklistStrings != null) {
						logger.info("Concept Blacklist is {}", conceptBlacklistStrings.toString());
						completedConceptIds.removeAll(stringsToLongs(conceptBlacklistStrings, new ArrayList<Long>()));
					}
				}

				// Create selection archive (automatically pulls in any new daily exports first and skips concepts with incomplete dependencies)
				SelectionResult selectionResult = createSelectionArchive(completedConceptIds, importResult);

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
						jiraContentProjectSync.addComment(taskKey, "Created task with selection from workbench daily export. SCTID list: " + toJiraSearchableIdList(selectionResult.getFoundConceptIds()));
						jiraContentProjectSync.updateStatus(taskKey, JiraTransitions.IMPORT_CONTENT);
					} else {
						handleError("Import process failed, see SnowOwl logs for details.", importResult);
						jiraContentProjectSync.updateStatus(taskKey, DailyDeltaTicketWorkflow.TRANSITION_FROM_CREATED_TO_REJECTED);
					}

					return importResult;
				} else {
					if (selectionResult.isEmptySelection()) {
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
			} catch (GooglePublishedSheetsClientException e) {
				return handleError("Failed to retrieve concept blacklist from Google Docs.", importResult, e);
			}
		} catch (JiraException e) {
			throw new ImporterException("Error using Jira.", e);
		}
	}

	public SelectionResult createSelectionArchive(Set<Long> completedConceptIds, ImportResult importResult) throws ImportFilterServiceException {
		Set<Long> conceptsWithMissingDependencies;
		try {
			conceptsWithMissingDependencies = backlogContentService.getConceptsWithMissingDependencies(completedConceptIds);
		} catch (IOException | LoadException e) {
			throw new ImportFilterServiceException("Error calculating component dependencies in the backlog content.", e);
		}

		if (conceptsWithMissingDependencies.isEmpty()) {
			addComment("No incomplete dependencies found.", importResult, "Info");
		} else {
			// Remove concepts with incomplete dependencies from the selection list
			completedConceptIds.removeAll(conceptsWithMissingDependencies);

			handleWarning("The following concepts are complete but will not be imported " +
					"because they are dependent on others which are not complete: \n" +
					toJiraSearchableIdList(conceptsWithMissingDependencies), importResult);
		}

		try {
			return importFilter.createFilteredImport(completedConceptIds);
		} catch (IOException e) {
			throw new ImportFilterServiceException("Error during creation of filtered archive.", e);
		}
	}

	private void handleWarning(String message, ImportResult importResult) {
		addComment(message, importResult, "Warning");
	}

	private ImportResult handleError(String message, ImportResult importResult, Exception e) {
		// If we have an Exception then it's an application error which needs logging as such.
		logger.error(message, e);
		return handleError(message, importResult);
	}
	private ImportResult handleError(String message, ImportResult importResult) {
		// No Exception here so must just be a user/content error.. no error logging needed.
		addComment(message, importResult, "Error");
		return importResult.setMessage(message);
	}

	private void addComment(String message, ImportResult importResult, String messageLevel) {
		try {
			jiraContentProjectSync.addComment(importResult.getTaskKey(), messageLevel + ": " + message);
		} catch (JiraException e) {
			logger.error("Failed to add {} comment to issue.", messageLevel, e);
		}
	}

	private String toJiraSearchableIdList(Set<Long> sctids) {
		StringBuilder builder = new StringBuilder();
		for (Long sctid : sctids) {
			if (builder.length() > 0) builder.append(", ");
			builder.append("|");
			builder.append(sctid.toString());
			builder.append("|");
		}
		return builder.toString();
	}

	private Collection<Long> stringsToLongs(Collection<String> strings, Collection<Long> longs) {
		for (String string : strings) {
			longs.add(Long.parseLong(string));
		}
		return longs;
	}

}
