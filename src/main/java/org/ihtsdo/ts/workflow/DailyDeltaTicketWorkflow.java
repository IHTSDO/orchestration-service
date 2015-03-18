package org.ihtsdo.ts.workflow;

import java.io.File;
import java.io.IOException;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.srs.client.SRSRestClient;
import org.ihtsdo.srs.client.SRSRestClientHelper;
import org.ihtsdo.ts.importer.JiraTransitions;
import org.ihtsdo.ts.importer.clients.jira.JQLBuilder;
import org.ihtsdo.ts.importer.clients.jira.JiraDataHelper;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.ClassificationResults;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

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

	private final JiraDataHelper jiraDataHelper;

	private final String interestingTicketsJQL;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String JIRA_DATA_MARKER = "WORKFLOW_DATA:";
	public static final String EXPORT_ARCHIVE_LOCATION = "Export File Location";

	public static final String TRANSITION_TO_EXPORTED = "Export Content";
	public static final String TRANSITION_TO_BUILT = "Run SRS build process";
	public static final String TRANSITION_TO_FAILED = "Failed";

	@Autowired
	public DailyDeltaTicketWorkflow(String jiraProjectKey) {
		interestingTicketsJQL = new JQLBuilder()
				.project(jiraProjectKey)
				.statusNot(State.FAILED.toString())
				.statusNot(State.PROMOTED.toString())
				.statusNot(State.PUBLISHED.toString())
				.statusNot(State.CLOSED.toString())
				.toString();
		jiraDataHelper = new JiraDataHelper(JIRA_DATA_MARKER);
	}

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	public void processChangedTicket(Issue issue) {
		State currentState = getState(issue);
		try {
			switch (currentState) {
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

				case ACCEPTED:
					mergeTaskToMain(issue);
					break;

				case PROMOTED:
					versionMain(issue);
					break;

				case FAILED:
					break;

				case PUBLISHED:
					break;

				case CLOSED:
					logger.debug("Ticket {} is in final state {}", issue.getKey(), currentState);
					break;

			}
		} catch (Exception e) {
			String errMsg = "Exception while processing ticket " + issue.getKey() + " at state " + currentState + ": " + e.getMessage();
			logger.error(errMsg, e);
			//Attempt to put the ticket into the failed State with a comment to that effect
			try {
				issue.addComment(errMsg);
				issue.transition().execute(JiraTransitions.FAILED);

			} catch (Exception e2) {
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
		jiraProjectSync.updateStatus(issue.getKey(), newStatus);
	}

	private void versionMain(Issue issue) {
		throw new NotImplementedException("Code not yet written to versionMain");
	}

	private void revertImport(Issue issue) {
		throw new NotImplementedException("Code not yet written to revertImport");
	}

	private void callSRS(Issue issue) throws JiraException, ProcessWorkflowException, IOException {
		String exportArchiveLocation = jiraDataHelper.getData(issue, EXPORT_ARCHIVE_LOCATION);
		Assert.notNull(exportArchiveLocation, EXPORT_ARCHIVE_LOCATION + " can not be null.");
		File exportArchive = new File(exportArchiveLocation);
		callSRS(exportArchive);
		issue.transition().execute(TRANSITION_TO_BUILT);
	}

	// Pulled out this section so it can be tested in isolation from Jira Issue
	public void callSRS(File exportArchive) throws ProcessWorkflowException, IOException {
		String releaseDate = SRSRestClientHelper.recoverReleaseDate(exportArchive);
		File srsFilesDir = SRSRestClientHelper.readyInputFiles(exportArchive, releaseDate);
		SRSRestClient.runDailyBuild(srsFilesDir, releaseDate);
	}

	private void exportTask(Issue issue) throws Exception {
		// SnowOwl does not yes support extracts from branches, so we'll just code for Main for now
		File exportArchive = tsClient.exportVersion(SnowOwlRestClient.MAIN, SnowOwlRestClient.EXTRACT_TYPE.DELTA);
		jiraDataHelper.putData(issue, EXPORT_ARCHIVE_LOCATION, exportArchive.getAbsolutePath());
		issue.transition().execute(TRANSITION_TO_EXPORTED);
	}

	private void awaitRVFResults(Issue issue) {
		throw new NotImplementedException("Code not yet written to awaitRVFResults");
	}
	
	private void mergeTaskToMain(Issue issue) {
		throw new NotImplementedException("Code not yet written to mergeTaskToMain");
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
		ACCEPTED,
		PROMOTED,
		PUBLISHED,
		FAILED,
		CLOSED
	}

}
