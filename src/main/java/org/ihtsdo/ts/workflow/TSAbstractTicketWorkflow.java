package org.ihtsdo.ts.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.rvf.client.RVFRestClient;
import org.ihtsdo.srs.client.SRSRestClient;
import org.ihtsdo.srs.client.SRSRestClientHelper;
import org.ihtsdo.ts.importer.JiraTransitions;
import org.ihtsdo.ts.importer.clients.jira.JiraDataHelper;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.ClassificationResults;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
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
	protected JiraProjectSync jiraProjectSync;

	@Autowired
	protected SnowOwlRestClient tsClient;

	@Autowired
	protected SRSRestClient srsClient;

	@Autowired
	protected RVFRestClient rvfClient;

	@Autowired
	protected JiraDataHelper jiraDataHelper;
	
	@Autowired
	private String exportDeltaStartEffectiveTime;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String JIRA_DATA_MARKER = "WORKFLOW_DATA:";
	public static final String EXPORT_ARCHIVE_LOCATION = "Export File Location";
	public static final String RVF_RESPONSE_URL = "RVF Response URL";
	public static final String CLASSIFICATION_ID = "Classification ID";

	public static final String TRANSITION_FROM_CREATED_TO_REJECTED = "Reject inconsistent data";
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

	protected void runClassifier(Issue issue, SnowOwlRestClient.BranchType branchType) throws SnowOwlRestClientException,
			InterruptedException, JiraException {
		ClassificationResults results = snowOwlRestClient.classify(issue.getKey(), branchType);
		boolean equivalentConceptsFound = results.isEquivalentConceptsFound();
		boolean relationshipChangesFound = results.isRelationshipChangesFound();
		String newStatus;
		String comment = "";
		if (!equivalentConceptsFound && !relationshipChangesFound) {
			newStatus = JiraTransitions.CLASSIFY_WITHOUT_QUERIES;
			comment = "No issues found by classifier.";
		} else {
			newStatus = JiraTransitions.CLASSIFY_WITH_QUERIES;
			if (equivalentConceptsFound) {
				comment += "Equivalent concepts found by classifier. ";
			}
			if (relationshipChangesFound) {
				comment += "Redundant stated relationships found by classifier.";
			}
		}
		issue.addComment(comment);
		jiraDataHelper.putData(issue, CLASSIFICATION_ID, results.getClassificationId());
		jiraProjectSync.updateStatus(issue, newStatus);
	}


	protected void callSRS(Issue issue) throws Exception {
		String exportArchiveLocation = jiraDataHelper.getData(issue, EXPORT_ARCHIVE_LOCATION);
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
		String releaseDate = SRSRestClientHelper.recoverReleaseDate(exportArchive);
		File srsFilesDir = SRSRestClientHelper.readyInputFiles(exportArchive, releaseDate);
		return srsClient.runDailyBuild(srsFilesDir, releaseDate);
	}

	protected void export(Issue issue, SnowOwlRestClient.BranchType exportFrom) throws Exception {

		File exportArchive;
		switch (exportFrom) {
			case MAIN:
				exportArchive = tsClient.exportVersion(SnowOwlRestClient.BranchType.MAIN.name(), SnowOwlRestClient.ExtractType.DELTA);
				break;
			case BRANCH:
				exportArchive = tsClient.exportBranch(issue.getKey(), SnowOwlRestClient.ExtractType.DELTA, exportDeltaStartEffectiveTime);
				break;
			default:
				throw new TicketWorkflowException("Export requested from unknown source: " + exportFrom.name());
		}
		jiraDataHelper.putData(issue, EXPORT_ARCHIVE_LOCATION, exportArchive.getAbsolutePath());
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_EXPORTED);
	}

	protected void awaitRVFResults(Issue issue) throws Exception {
		String rvfResponseURL = jiraDataHelper.getData(issue, RVF_RESPONSE_URL);
		rvfClient.waitForResults(rvfResponseURL);
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_VALIDATED);
		issue.addComment("Release validation ready to view at: " + rvfResponseURL);
	}
	
	protected void mergeTaskToMain(Issue issue) throws IOException, JSONException, JiraException {
		snowOwlRestClient.promoteBranch(issue.getKey());
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_PROMOTED);
	}

	protected void exportTask(Issue issue) throws Exception {
		File exportArchive = tsClient.exportBranch(issue.getKey(), SnowOwlRestClient.ExtractType.DELTA, exportDeltaStartEffectiveTime);
		jiraDataHelper.putData(issue, EXPORT_ARCHIVE_LOCATION, exportArchive.getAbsolutePath());
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_EXPORTED);
	}

	protected void versionMain(Issue issue) {
		throw new NotImplementedException("Code not yet written to versionMain");
	}

	protected String jiraState(Enum state) {
		return "\"" + state.name().replace("_", " ") + "\"";
	}
}
