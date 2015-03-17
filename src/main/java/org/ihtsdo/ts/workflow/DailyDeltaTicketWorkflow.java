package org.ihtsdo.ts.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.ts.importer.JiraTransitions;
import org.ihtsdo.ts.importer.clients.jira.JQLBuilder;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.ihtsdo.ts.importer.clients.snowowl.ClassificationResults;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

@Resource
public class DailyDeltaTicketWorkflow implements TicketWorkflow {

	@Autowired
	private SnowOwlRestClient snowOwlRestClient;

	@Autowired
	private JiraProjectSync jiraProjectSync;

	private final String interestingTicketsJQL;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	public DailyDeltaTicketWorkflow(String jiraProjectKey) {
		interestingTicketsJQL = new JQLBuilder()
				.project(jiraProjectKey)
				.statusNot(State.FAILED.toString())
				.statusNot(State.PROMOTED.toString())
				.statusNot(State.PUBLISHED.toString())
				.statusNot(State.CLOSED.toString())
				.toString();
	}

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	public void processChangedTicket(Issue issue) {
		State currentState = getState(issue);
		try{
			switch (currentState) {
				case IMPORTED:	runClassifier(issue);
								break;

				case CLASSIFIED_WITH_QUERIES:	logger.info("Ticket {} has classification queries.  Awaiting user action", issue.getKey());
												break;

				case CLASSIFIED_SUCCESSFULLY:	exportTask();
												break;

				case EXPORTED:	callSRS();
								break;

				case BUILT:		awaitRVFResults();
								break;

				case VALIDATED: logger.info("Ticket {} is awaiting user acceptance of validation.", issue.getKey());
								break;

				case REJECTED:	revertImport();

				case ACCEPTED:	mergeTaskToMain();
								break;

				case PROMOTED:	versionMain();
								break;

				case FAILED:
				case PUBLISHED:
				case CLOSED:	logger.debug("Ticket {} is in final state {}", issue.getKey(), currentState);
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

	private void versionMain() {
		throw new NotImplementedException("Code not yet written to versionMain");
	}

	private void revertImport() {
		throw new NotImplementedException("Code not yet written to revertImport");
	}

	private void callSRS() {
		throw new NotImplementedException("Code not yet written to callSRS");
	}

	private void exportTask() {
		throw new NotImplementedException("Code not yet written to exportTask");
	}

	private void awaitRVFResults() {
		throw new NotImplementedException("Code not yet written to awaitRVFResults");
	}
	
	private void mergeTaskToMain() {
		throw new NotImplementedException("Code not yet written to mergeTaskToMain");
	}

	private static enum State {
		CREATED,
		IMPORTED,
		CLASSIFIED_SUCCESSFULLY,
		CLASSIFIED_WITH_QUERIES,
		EXPORTED,
		BUILT,
		VALIDATED,
		ACCEPTED,
		REJECTED,
		PROMOTED,
		PUBLISHED,
		FAILED,
		CLOSED
	}
}
