package org.ihtsdo.ts.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.rvf.client.RVFRestClient;
import org.ihtsdo.srs.client.SRSRestClient;
import org.ihtsdo.srs.client.SRSRestClientHelper;
import org.ihtsdo.ts.importer.Importer;
import org.ihtsdo.ts.importer.JiraTransitions;
import org.ihtsdo.ts.importer.clients.jira.JQLBuilder;
import org.ihtsdo.ts.importer.clients.jira.JiraDataHelper;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.ClassificationResults;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
import org.ihtsdo.ts.importfilter.ImportFilterService;
import org.ihtsdo.ts.importfilter.ImportFilterServiceException;
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
public class DailyDeltaTicketWorkflow implements TicketWorkflow {

	@Autowired
	private SnowOwlRestClient snowOwlRestClient;

	@Autowired
	private JiraProjectSync jiraProjectSync;

	@Autowired
	private SnowOwlRestClient tsClient;

	@Autowired
	private SRSRestClient srsClient;

	@Autowired
	private RVFRestClient rvfClient;

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraDataHelper jiraDataHelper;

	private final String interestingTicketsJQL;

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

	@Autowired
	public DailyDeltaTicketWorkflow(String jiraProjectKey) {
		interestingTicketsJQL = new JQLBuilder()
				.project(jiraProjectKey)
				.statusNot(State.CREATED.toString())
				.statusNot(State.FAILED.toString())
				.statusNot(State.PROMOTED.toString())
				.statusNot(State.CLOSED.toString())
				.toString();
	}

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	synchronized public void processChangedTicket(Issue issue) {
		State currentState = getState(issue);
		try {
			switch (currentState) {
				case CREATED:
					logger.info("DailyDelta Workflow ignoring ticket at status CREATED: {}", issue.getKey());
					break;
				case IMPORTED:
					runClassifier(issue);
					break;

				case CLASSIFIED_WITH_QUERIES:
					logger.info("Ticket {} has classification queries.  Awaiting user action", issue.getKey());
					break;

				case CONTENT_REJECTED:
					revertImport(issue);
					break;

				case CLASSIFIED_SUCCESSFULLY:
					exportTask(issue);
					break;

				case EXPORTED:
					callSRS(issue);
					break;

				case BUILT:
					awaitRVFResults(issue);
					break;

				case VALIDATED:
					logger.info("Ticket {} is awaiting user acceptance of validation.", issue.getKey());
					break;

			case VALIDATION_ACCEPTED:
					mergeTaskToMain(issue);
					break;

				case PROMOTED:
					versionMain(issue);
					break;

				case FAILED:
					break;

				case CLOSED:
					logger.debug("Ticket {} is in final state {}", issue.getKey(), currentState);
					break;

			}
		} catch (Exception e) {
			String errMsg = "Exception while processing ticket " + issue.getKey() + " at state " + currentState + ": " + e.getMessage();
			logger.error(errMsg, e);
			// Is there a cause that we could add to the ticket?
			Throwable cause = e.getCause();
			if (cause != null) {
				errMsg += "\nCaused by: " + cause.getMessage();
			}
			//Attempt to put the ticket into the failed State with a comment to that effect
			try {
				issue.addComment(errMsg);
				issue.transition().execute(JiraTransitions.FAILED);
			} catch (JiraException e2) {
				logger.error("Additional exception while trying to record previous exception in Jira.", e2);
			}
		}
	}

	private State getState(Issue issue) {
		return State.valueOf(issue.getStatus().getName().toUpperCase().replace(" ", "_"));
	}

	private void runClassifier(Issue issue) throws SnowOwlRestClientException, InterruptedException, JiraException {
		ClassificationResults results = snowOwlRestClient.classify(issue.getKey());
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
		jiraProjectSync.updateStatus(issue.getKey(), newStatus);
	}

	private void revertImport(Issue issue) throws JiraException, ImportFilterServiceException {
		String selectedArchiveVersion = jiraDataHelper.getData(issue, Importer.SELECTED_ARCHIVE_VERSION);
		Assert.notNull(selectedArchiveVersion, "Selected archive version can not be null.");
		importFilterService.putSelectionArchiveBackInBacklog(selectedArchiveVersion);
		jiraProjectSync.updateStatus(issue.getKey(), TRANSITION_TO_CLOSED);
	}

	private void callSRS(Issue issue) throws Exception {
		String exportArchiveLocation = jiraDataHelper.getData(issue, EXPORT_ARCHIVE_LOCATION);
		Assert.notNull(exportArchiveLocation, EXPORT_ARCHIVE_LOCATION + " can not be null.");
		File exportArchive = new File(exportArchiveLocation);
		Map<String, String> srsResponse = callSRS(exportArchive);
		jiraProjectSync.updateStatus(issue.getKey(), TRANSITION_TO_BUILT);
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

	private void exportTask(Issue issue) throws Exception {
		File exportArchive = tsClient.exportBranch(issue.getKey(), SnowOwlRestClient.EXTRACT_TYPE.DELTA);
		jiraDataHelper.putData(issue, EXPORT_ARCHIVE_LOCATION, exportArchive.getAbsolutePath());
		jiraProjectSync.updateStatus(issue.getKey(), TRANSITION_TO_EXPORTED);
	}

	private void awaitRVFResults(Issue issue) throws Exception {
		String rvfResponseURL = jiraDataHelper.getData(issue, RVF_RESPONSE_URL);
		rvfClient.waitForResults(rvfResponseURL);
		jiraProjectSync.updateStatus(issue.getKey(), TRANSITION_TO_VALIDATED);
		issue.addComment("Release validation ready to vies at: " + rvfResponseURL);
	}
	
	private void mergeTaskToMain(Issue issue) throws IOException, JSONException, JiraException {
		snowOwlRestClient.promoteBranch(issue.getKey());
		jiraProjectSync.updateStatus(issue.getKey(), TRANSITION_TO_PROMOTED);
	}

	private void versionMain(Issue issue) {
		throw new NotImplementedException("Code not yet written to versionMain");
	}

	private static enum State {
		CREATED,
		IMPORTED,
		CLASSIFIED_WITH_QUERIES,
		CONTENT_REJECTED,
		CLASSIFIED_SUCCESSFULLY,
		EXPORTED,
		BUILT,
		VALIDATED,
		VALIDATION_ACCEPTED,
		PROMOTED,
		FAILED,
		CLOSED
	}

	private static State[] COMPLETE_STATES = { State.PROMOTED, State.FAILED, State.CLOSED };

	@Override
	public boolean isComplete(Issue issue) {
		boolean isComplete = false;
		// Loop through to see if we're currently in a complete state
		State currentState = getState(issue);
		for (State thisCompleteState : COMPLETE_STATES) {
			if (currentState == thisCompleteState) {
				isComplete = true;
			}
		}
		return isComplete;
	}

}
