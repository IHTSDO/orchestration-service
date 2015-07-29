package org.ihtsdo.orchestration.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.orchestration.clients.srs.SRSFileDAO;
import org.ihtsdo.orchestration.clients.jira.JiraDataHelper;
import org.ihtsdo.orchestration.clients.jira.JiraSyncException;
import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.clients.jira.JiraProjectSync;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.ClassificationResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import us.monoid.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;

@Resource
public abstract class TSAbstractTicketWorkflow implements TicketWorkflow {

	@Autowired
	protected SnowOwlRestClient snowOwlRestClient;

	@Autowired
	private String snowowlProjectBranch;

	@Autowired
	protected JiraProjectSync jiraProjectSync;

	@Autowired
	protected SRSRestClient srsClient;

	@Autowired
	protected RVFRestClient rvfClient;

	@Autowired
	protected JiraDataHelper jiraDataHelper;
	
	@Autowired
	protected SRSFileDAO srsDAO;

	@Autowired
	private String exportDeltaStartEffectiveTime;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String JIRA_DATA_MARKER = "WORKFLOW_DATA:";
	public static final String EXPORT_ARCHIVE_LOCATION = "Export File Location";
	public static final String RVF_RESPONSE_URL = "RVF Response URL";
	public static final String CLASSIFICATION_ID = "Classification ID";

	public static final String TRANSITION_FROM_CREATED_TO_CLOSED = "Nothing to import";
	public static final String TRANSITION_FROM_CREATED_TO_REJECTED = "Reject inconsistent data";
	public static final String TRANSITION_FROM_CLASSIFICATION_ACCEPTED_TO_SUCCESS = "Classification Saved";
	public static final String TRANSITION_TO_EXPORTED = "Export content";
	public static final String TRANSITION_TO_BUILT = "Run SRS build process";
	public static final String TRANSITION_TO_FAILED = "Failed";
	public static final String TRANSITION_TO_CLOSED = "Close task";
	public static final String TRANSITION_TO_VALIDATED = "Run through RVF";
	public static final String TRANSITION_TO_PROMOTED = "Promote content to MAIN";

	@Override
	public abstract String getInterestingTicketJQLSelectStatement();

	@Override
	public abstract void processChangedTicket(Issue issue);

	protected abstract Enum[] getCompleteStates();

	protected abstract Enum getState(Issue issue);

	@Override
	public boolean isComplete(Issue issue) {
		boolean isComplete = false;
		// Loop through to see if we're currently in a complete state
		Enum currentState = getState(issue);
		for (Enum thisCompleteState : getCompleteStates()) {
			if (currentState == thisCompleteState) {
				isComplete = true;
			}
		}
		return isComplete;
	}

	protected void runClassifier(Issue issue, BranchType branchType) throws RestClientException,
			InterruptedException, JiraException, JiraSyncException, TicketWorkflowException {
		String issueKey = issue.getKey();

		ClassificationResults results;
		switch (branchType) {
			case PROJECT:
				results = snowOwlRestClient.classify(snowowlProjectBranch);
				break;
			case TASK:
				results = snowOwlRestClient.classify(snowowlProjectBranch + "/" + issueKey);
				break;
			default:
				throw new TicketWorkflowException("Unrecognised branch type " + branchType.name());
		}

		boolean equivalentConceptsFound = results.isEquivalentConceptsFound();
		boolean relationshipChangesFound = results.isRelationshipChangesFound();
		String statusTransition;
		if (!equivalentConceptsFound && !relationshipChangesFound) {
			statusTransition = JiraTransitions.CLASSIFY_WITHOUT_QUERIES;
			jiraProjectSync.addComment(issueKey, "No issues found by classifier.");
		} else {
			statusTransition = JiraTransitions.CLASSIFY_WITH_QUERIES;
			if (equivalentConceptsFound) {
				jiraProjectSync.addComment(issueKey, "Equivalent concepts found by classifier.\n" + results.getEquivalentConceptsJson());
			}
			if (relationshipChangesFound) {
				jiraProjectSync.addComment(issueKey, results.getRelationshipChangesCount() + " relationship changes found by classifier. See attachment " + results.getRelationshipChangesFile().getName());
				jiraProjectSync.attachFile(issueKey, results.getRelationshipChangesFile());
			}
		}
		jiraDataHelper.putData(issue, CLASSIFICATION_ID, results.getClassificationId());
		jiraProjectSync.updateStatus(issue, statusTransition);
	}

	protected void saveClassification(Issue issue, BranchType branchType) throws JiraException,
			RestClientException, JiraSyncException, TicketWorkflowException, InterruptedException {
		String classificationId = jiraDataHelper.getLatestData(issue, CLASSIFICATION_ID);
		snowOwlRestClient.saveClassification(issue.getKey(), classificationId);
		jiraProjectSync.updateStatus(issue, TRANSITION_FROM_CLASSIFICATION_ACCEPTED_TO_SUCCESS);
	}

	protected void callSRS(Issue issue) throws Exception {
		String exportArchiveLocation = jiraDataHelper.getLatestData(issue, EXPORT_ARCHIVE_LOCATION);
		Assert.notNull(exportArchiveLocation, EXPORT_ARCHIVE_LOCATION + " can not be null.");
		File exportArchive = new File(exportArchiveLocation);
		Map<String, String> srsResponse = callSRS(exportArchive);
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_BUILT);
		// Can we store the RVF location for the next step in the process to poll?
		if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
			jiraDataHelper.putData(issue, RVF_RESPONSE_URL, srsResponse.get(SRSRestClient.RVF_RESPONSE));
		} else {
			logger.warn("Did not find RVF Response location in SRS Client Response");
		}
		jiraProjectSync.addComment(issue.getKey(), "The build process returned the following items of interest: ", srsResponse);
	}

	// Pulled out this section so it can be tested in isolation from Jira Issue
	public Map<String, String> callSRS(File exportArchive) throws Exception {
		String releaseDate = srsDAO.recoverReleaseDate(exportArchive);
		boolean includeExternallyMaintainedRefsets = true;
		File srsFilesDir = srsDAO.readyInputFiles(exportArchive, releaseDate, includeExternallyMaintainedRefsets);
		return srsClient.runDailyBuild(srsFilesDir, releaseDate);
	}

	protected void export(Issue issue, BranchType exportFrom) throws Exception {

		File exportArchive;
		switch (exportFrom) {
			case PROJECT:
			exportArchive = snowOwlRestClient.exportProject(snowowlProjectBranch, SnowOwlRestClient.ExtractType.DELTA);
				break;
			case TASK:
			exportArchive = snowOwlRestClient.exportTask(snowowlProjectBranch, issue.getKey(), SnowOwlRestClient.ExtractType.DELTA);
				break;
			default:
				throw new TicketWorkflowException("Export requested from unknown source: " + exportFrom.name());
		}
		jiraDataHelper.putData(issue, EXPORT_ARCHIVE_LOCATION, exportArchive.getAbsolutePath());
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_EXPORTED);
	}

	protected void awaitRVFResults(Issue issue) throws Exception {
		String rvfResponseURL = jiraDataHelper.getLatestData(issue, RVF_RESPONSE_URL);
		rvfClient.waitForResults(rvfResponseURL);
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_VALIDATED);
		issue.addComment("Release validation ready to view at: " + rvfResponseURL);
	}
	
	protected void mergeTaskToProject(Issue issue) throws IOException, JSONException, JiraException, JiraSyncException {
		snowOwlRestClient.mergeTaskToProject(snowowlProjectBranch, issue.getKey());
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_PROMOTED);
	}

	protected void versionMain(Issue issue) {
		throw new NotImplementedException("Code not yet written to versionMain");
	}

	protected String jiraState(Enum state) {
		return "\"" + state.name().replace("_", " ") + "\"";
	}

	public enum BranchType {
		PROJECT, TASK
	}
}
