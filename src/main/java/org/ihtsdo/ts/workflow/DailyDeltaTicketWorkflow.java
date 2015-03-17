package org.ihtsdo.ts.workflow;

import net.rcarz.jiraclient.Issue;
import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.ts.importer.clients.jira.JQLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

@Resource
public class DailyDeltaTicketWorkflow implements TicketWorkflow {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String interestingTicketsJQL;

	@Autowired
	public DailyDeltaTicketWorkflow(String jiraProjectKey) {
		interestingTicketsJQL = new JQLBuilder().project(jiraProjectKey).statusNot(State.PROMOTED.toString()).toString();
	}

	public static enum State {
		CREATED,
		IMPORTED,
		CLASSIFICATION_QUERIES,
		CLASSIFICATION_SUCCESS,
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

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	public void processChangedTicket(Issue issue) {
		State currentState = State.valueOf(issue.getStatus().getName().toUpperCase());
		try{
			switch (currentState) {
				case CREATED:	doImport();
								break;

				case IMPORTED:	runClassifier();
								break;

				case CLASSIFICATION_QUERIES:	logger.info("Ticket {} has classification queries.  Awaiting user action", issue.getKey());
												break;

				case CLASSIFICATION_SUCCESS:	exportTask();
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
//				issue.transition().execute(State.FAILED.name()); TODO: Reinstate this once the transition is implemented in Jira
			} catch (Exception e2) {
				logger.error("Additional exception while trying to record previous exception in Jira.", e2);
			}
		}
	}

	private void doImport() {
		throw new NotImplementedException("Code not yet written to doImport");
	}

	private void runClassifier() {
		throw new NotImplementedException("Code not yet written to runClassifier");
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
}
