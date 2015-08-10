package org.ihtsdo.orchestration.importer;

import com.b2international.commons.VerhoeffCheck;

import net.rcarz.jiraclient.JiraException;

import org.apache.commons.lang.time.FastDateFormat;
import org.ihtsdo.orchestration.clients.jira.JiraDataHelper;
import org.ihtsdo.orchestration.clients.jira.JiraProjectSync;
import org.ihtsdo.orchestration.clients.jira.JiraSyncException;
import org.ihtsdo.orchestration.clients.snowowl.ImportError;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.orchestration.clients.workbenchdata.WorkbenchWorkflowClient;
import org.ihtsdo.orchestration.clients.workbenchdata.WorkbenchWorkflowClientException;
import org.ihtsdo.orchestration.workflow.DailyDeltaTicketWorkflow;
import org.ihtsdo.orchestration.workflow.JiraTransitions;
import org.ihtsdo.ts.importfilter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.*;

public class Importer {

	public static final String SELECTED_ARCHIVE_VERSION = "SelectedArchiveVersion";

	private static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss");

	@Autowired
	private WorkbenchWorkflowClient workbenchWorkflowClient;

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraProjectSync jiraContentProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	@Autowired
	private String snowowlProjectBranch;

	@Autowired
	private JiraDataHelper jiraDataHelper;

	@Autowired
	private BacklogContentService backlogContentService;

	@Autowired
	private ImportFilter importFilter;

	@Autowired
	private ImportBlacklistService importBlacklistService;

	@Autowired
	private String jiraProjectKey;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ImportResult importSelectedWBContent(Set<Long> selectConceptIdsOverride) throws ImporterException {
		return importWBContent(selectConceptIdsOverride, false);
	}

	public ImportResult importAllWBContent() throws ImporterException {
		return importWBContent(null, true);
	}

	private ImportResult importWBContent(Set<Long> selectConceptIdsOverride, boolean importEverything) throws ImporterException {
		logger.info("Started");
		Date startDate = new Date();
		String contentEffectiveDate = ""; // Blank effective date allows importing into Snow Owl as unpublished content
		ImportResult importResult = new ImportResult();

		try {
			// Create Jira issue
			String taskKey = jiraContentProjectSync.createTask(jiraProjectKey, SIMPLE_DATE_FORMAT.format(startDate));
			importResult.setTaskKey(taskKey);

			try {
				importFilterService.importNewWorkbenchArchives();

				logger.info("Fetching Workbench workflow concept IDs with approval status.");
				Map<Long, Boolean> conceptIdsWithApprovalStatus = workbenchWorkflowClient.getConceptIdsWithApprovedStatus();

				Set<Long> idsWithInvalidVerhoeffCheckDigit = getIdsWithInvalidVerhoeffCheckDigit(conceptIdsWithApprovalStatus.keySet());
				if (!idsWithInvalidVerhoeffCheckDigit.isEmpty()) {
					jiraContentProjectSync.addComment(taskKey, "The following SCT IDs have an invalid verhoeff check digit and will not be imported (will be treated as incomplete): " + idsWithInvalidVerhoeffCheckDigit.toString());
					for (Long id : idsWithInvalidVerhoeffCheckDigit) {
						conceptIdsWithApprovalStatus.remove(id);
					}
				}

				Set<Long> completedConceptIds;
				Set<Long> incompleteConceptIds = workbenchWorkflowClient.getIncompleteConceptIds(conceptIdsWithApprovalStatus);
				incompleteConceptIds.addAll(idsWithInvalidVerhoeffCheckDigit);

				if (selectConceptIdsOverride != null) {
					completedConceptIds = selectConceptIdsOverride;
					jiraContentProjectSync.addComment(taskKey, "Concept selection override, SCTID list: \n" + toJiraSearchableIdList(selectConceptIdsOverride));
					logger.info("Incomplete concepts ({}): {}", incompleteConceptIds.size(), incompleteConceptIds);
					importSelection(taskKey, completedConceptIds, incompleteConceptIds, importResult, true, contentEffectiveDate);
				} else {

					if (importEverything) {
						jiraContentProjectSync.addComment(taskKey, "Attempting to import everything in the backlog, without blacklist at first.");
						completedConceptIds = null;
						incompleteConceptIds = new HashSet<>();
					} else {
						jiraContentProjectSync.addComment(taskKey, "Attempting to import selected content, without blacklist at first.");
						completedConceptIds = workbenchWorkflowClient.getCompleteConceptIds(conceptIdsWithApprovalStatus);
						logger.info("Completed concepts ({}): {}", completedConceptIds.size(), completedConceptIds);
						logger.info("Incomplete concepts ({}): {}", incompleteConceptIds.size(), incompleteConceptIds);
					}

					importSelection(taskKey, completedConceptIds, incompleteConceptIds, importResult, importEverything, contentEffectiveDate);

					if (importEverything) {
						completedConceptIds = importResult.getSelectionResult().getFoundConceptIds();
					}

					// Iterate attempting import and adding to blacklist until import is successful
					for (int blacklistRun = 1;
						 (importResult.getSelectionResult() != null && importResult.getSelectionResult().isSuccess())
								 && !importResult.isImportCompletedSuccessfully()
								 && !importResult.isBuildingBlacklistFailed()
								 && blacklistRun <= 10; blacklistRun++) {

						jiraContentProjectSync.addComment(taskKey, "Import failed, rolling back selection and building blacklist from import errors.");
						importFilterService.putSelectionArchiveBackInBacklog(importResult.getSelectionResult().getSelectedArchiveVersion());
						importResult = new ImportResult(taskKey);
						try {
							ImportBlacklistResults blacklistResults = importBlacklistService.createBlacklistFromLatestImportErrors();
							List<Long> blacklistedConceptsFromThisRun = blacklistResults.getBlacklistedConcepts();
							if (!blacklistedConceptsFromThisRun.isEmpty()) {
								String blacklistMessage = (blacklistRun == 1 ? "Import blacklist" : "Additional import blacklist") + " (" + blacklistedConceptsFromThisRun.size() + " concepts):";
								jiraContentProjectSync.addComment(taskKey, blacklistMessage + "\n" + toJiraTable(blacklistResults.getImportErrors()));
								logger.info(blacklistMessage + " {}", blacklistedConceptsFromThisRun);
								completedConceptIds.removeAll(blacklistedConceptsFromThisRun);
								incompleteConceptIds.addAll(blacklistedConceptsFromThisRun);
								logger.info("Completed concepts minus blacklist ({}): {}", completedConceptIds.size(), completedConceptIds);
								jiraContentProjectSync.addComment(taskKey, "Reimporting content using new blacklist (attempt " + (blacklistRun + 1) + ").");
								importSelection(taskKey, completedConceptIds, incompleteConceptIds, importResult, false, contentEffectiveDate);
							} else {
								importResult.setBuildingBlacklistFailed(true);
								handleError("Import failed but no concept blacklist could be built.", importResult);
								updateStatus(taskKey, DailyDeltaTicketWorkflow.TRANSITION_TO_FAILED);
							}
						} catch (ImportBlacklistServiceException e) {
							importResult.setBuildingBlacklistFailed(true);
							handleExceptionTransitionToRejected("Failed to parse import error log.", importResult, e);
						}
					}
				}
				return importResult;
			} catch (ImportFilterServiceException e) {
				return handleExceptionTransitionToRejected("Error during selection archive creation process.", importResult, e);
			} catch (RestClientException e) {
				return handleExceptionTransitionToRejected("Error using Snow Owl Terminology Server.", importResult, e);
			} catch (WorkbenchWorkflowClientException e) {
				return handleExceptionTransitionToRejected(e.getMessage(), importResult, e);
			}
		} catch (JiraException | JiraSyncException e) {
			throw new ImporterException("Error using Jira.", e);
		}
	}

	private ImportResult importSelection(String taskKey, Set<Long> completedConceptIds, Set<Long> incompleteConceptIds, ImportResult importResult,
			boolean updateJiraOnImportError, String contentEffectiveDate) throws RestClientException, JiraException, ImportFilterServiceException,
			JiraSyncException {
		SelectionResult selectionResult = createSelectionArchive(completedConceptIds, incompleteConceptIds, importResult, contentEffectiveDate);
		importResult.setSelectionResult(selectionResult);

		for (String rowDiscardedMessage : selectionResult.getRowsDiscarded()) {
			jiraContentProjectSync.addComment(taskKey, rowDiscardedMessage);
		}

		if (selectionResult.isSuccess()) {
			// Creating a branch can fail, but we need to know which archive to roll back if it does, so capture this first
			String selectedArchiveVersion = selectionResult.getSelectedArchiveVersion();
			jiraDataHelper.putData(jiraContentProjectSync.findIssue(taskKey), SELECTED_ARCHIVE_VERSION, selectedArchiveVersion);
			logger.info("Filter version {}", selectedArchiveVersion);

			// Create TS branch
			tsClient.createProjectBranchIfNeeded(snowowlProjectBranch);
			tsClient.createProjectTaskIfNeeded(snowowlProjectBranch, taskKey);

			// Stream selection archive into TS import process
			InputStream selectionArchiveStream = importFilterService.getSelectionArchive(selectedArchiveVersion);
			boolean importSuccessful = tsClient.importRF2Archive(snowowlProjectBranch, taskKey, selectionArchiveStream);
			if (importSuccessful) {
				importResult.setImportCompletedSuccessfully(true);
				Set<Long> foundConceptIds = selectionResult.getFoundConceptIds();
				jiraContentProjectSync.addComment(taskKey, "Created task with selection from workbench daily export. SCTID list (" + foundConceptIds.size() + " concepts): \n" + toJiraSearchableIdList(foundConceptIds));
				updateStatus(taskKey, JiraTransitions.IMPORT_CONTENT);
			} else {
				if (updateJiraOnImportError) {
					handleError("Import process failed, see SnowOwl logs for details.", importResult);
					updateStatus(taskKey, DailyDeltaTicketWorkflow.TRANSITION_FROM_CREATED_TO_REJECTED);
				}
			}

			return importResult;
		} else {
			if (selectionResult.isEmptySelection()) {
				updateStatus(taskKey, DailyDeltaTicketWorkflow.TRANSITION_FROM_CREATED_TO_CLOSED);
				return handleError("The current selection did not match anything in the backlog.", importResult);
			} else {
				updateStatus(taskKey, DailyDeltaTicketWorkflow.TRANSITION_FROM_CREATED_TO_REJECTED);
				return handleError("Unknown selection problem.", importResult);
			}
		}
	}

	private SelectionResult createSelectionArchive(Set<Long> completedConceptIds, Set<Long> incompleteConceptIds, ImportResult importResult, String contentEffectiveDate) throws ImportFilterServiceException {
		if (completedConceptIds != null) {
			MissingDependencyReport missingDependencyReport;
			try {
				missingDependencyReport = backlogContentService.getConceptsWithMissingDependencies(completedConceptIds, incompleteConceptIds);
			} catch (IOException | LoadException e) {
				throw new ImportFilterServiceException("Error calculating component dependencies in the backlog content.", e);
			}

			if (!missingDependencyReport.anyMissingDependenciesFound()) {
				addComment("No incomplete dependencies found.", importResult, "Info");
			} else {
				// Remove concepts with incomplete dependencies from the selection list
				recordConceptsAsNotComplete(completedConceptIds, incompleteConceptIds, missingDependencyReport);

				handleWarning("The following concepts are complete but will not be imported " +
						"because they are related to others which are not complete: \n" +
						formatForJira(missingDependencyReport), importResult);

				try {
					List<MissingDependencyReport> exhaustiveReports = backlogContentService.getExhaustiveListOfConceptsWithMissingDependencies(completedConceptIds, incompleteConceptIds);

					// Remove concepts with incomplete dependencies from the selection list
					for (MissingDependencyReport exhaustiveReport : exhaustiveReports) {
						recordConceptsAsNotComplete(completedConceptIds, incompleteConceptIds, exhaustiveReport);
					}

					if (!exhaustiveReports.isEmpty()) {
						handleWarning("As a result of the exclusions above the following concepts will also not be included for the same " +
								"reason. Each row represents exclusions from a new iteration based on the results of the last: \n" +
								formatForJira(exhaustiveReports), importResult);
					}
				} catch (IOException | LoadException e) {
					throw new ImportFilterServiceException("Error calculating component dependencies in the backlog content.", e);
				}

			}
		}

		try {
			return importFilter.createFilteredImport(completedConceptIds, contentEffectiveDate);
		} catch (IOException | ImportFilterException e) {
			throw new ImportFilterServiceException("Error during creation of filtered archive.", e);
		}
	}

	/**
	 * Returns any invalid ids or an empty set.
	 */
	private Set<Long> getIdsWithInvalidVerhoeffCheckDigit(Set<Long> ids) {
		Set<Long> invalidIds = new HashSet<>();
		for (Long id : ids) {
			if (!VerhoeffCheck.validateLastChecksumDigit(id.toString())) {
				invalidIds.add(id);
			}
		}
		return invalidIds;
	}

	private void recordConceptsAsNotComplete(Set<Long> completedConceptIds, Set<Long> incompleteConceptIds, MissingDependencyReport missingDependencyReport) {
		for (Concept conceptWithMissingDependency : missingDependencyReport.getConceptToMissingStatedAndInferredDependencyMap().keySet()) {
			Long sctid = conceptWithMissingDependency.getSctid();
			completedConceptIds.remove(sctid);
			incompleteConceptIds.add(sctid);
		}
	}

	private String formatForJira(MissingDependencyReport missingDependencyReport) {
		StringBuilder builder = new StringBuilder();

		Map<Concept, List<Concept>> conceptToMissingDependencyMap = missingDependencyReport.getConceptToMissingStatedAndInferredDependencyMap();
		builder.append("||Complete concept excluded from import||Related incomplete concepts (arrow indicates direction of relation)||\n");
		for (Concept concept : conceptToMissingDependencyMap.keySet()) {
			builder.append("|").append(concept.getSctid()).append("|");
			boolean first = true;
			for (Concept incompleteConcept : conceptToMissingDependencyMap.get(concept)) {
				if (first) {
					first = false;
				} else {
					builder.append("\n");
				}
				builder.append(concept.getPathToRelationAsString(incompleteConcept));
			}
			builder.append("|\n");
		}

		return builder.toString();
	}

	private String formatForJira(List<MissingDependencyReport> exhaustiveReports) {
		StringBuilder builder = new StringBuilder();
		builder.append("||Further excluded concepts||\n");
		for (MissingDependencyReport exhaustiveReport : exhaustiveReports) {
			builder.append("|");
			Set<Concept> concepts = exhaustiveReport.getConceptToMissingStatedAndInferredDependencyMap().keySet();
			boolean first = true;
			for (Concept concept : concepts) {
				if (!first) builder.append(", ");
				builder.append(concept.getSctid());
				builder.append(" ");
				first = false;
			}
			builder.append("|\n");
		}
		return builder.toString();
	}

	private void handleWarning(String message, ImportResult importResult) {
		addComment(message, importResult, "Warning");
	}

	private void updateStatus(String taskKey, String statusTransitionName) throws JiraSyncException, JiraException {
		jiraContentProjectSync.updateStatus(taskKey, statusTransitionName);
	}

	private ImportResult handleExceptionTransitionToRejected(String message, ImportResult importResult, Exception e) {
		// If we have an Exception then it's an application error which needs logging as such.
		// But we still need to return the selected items to the backlog.
		logger.error(message, e);
		handleError(message, importResult, e);
		try {
			jiraContentProjectSync.conditionalUpdateStatus(importResult.getTaskKey(),
					DailyDeltaTicketWorkflow.TRANSITION_FROM_CREATED_TO_REJECTED, DailyDeltaTicketWorkflow.DDState.FAILED.toString());
		} catch (JiraException | JiraSyncException e2) {
			logger.error("Failed to transition issue {} to CONTENT_REJECTED state.", importResult.getTaskKey(), e2);
		}
		return importResult;
	}

	private ImportResult handleError(String message, ImportResult importResult, Exception e) {
		message += "\nCaused by Exception: " + e.getLocalizedMessage();
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

	private String toJiraSearchableIdList(Collection<Long> sctids) {
		StringBuilder builder = new StringBuilder();
		for (Long sctid : sctids) {
			if (builder.length() == 0) builder.append("|");
			builder.append(sctid.toString());
			builder.append("|");
		}
		return builder.toString();
	}

	private String toJiraTable(List<ImportError> importErrors) {
		StringBuilder builder = new StringBuilder();
		builder.append("||Concept ID||Import error message||\n");
		for (ImportError importError : importErrors) {
			builder.append("|").append(importError.getConceptId()).append("|");
			builder.append(importError.getMessage().replace("|", "-")).append("|\n");
		}
		return builder.toString();
	}
}
