package org.ihtsdo.ts.workflow;

import javax.annotation.Resource;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcarz.jiraclient.Issue;

@Resource
public class DailyDeltaTicketWorkflow extends TicketWorkflow {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
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

	public DailyDeltaTicketWorkflow() {
		workflowName = "DailyDelta";
	}

	protected void doStateMachine(Issue issue) {
		State currentState = State.valueOf(issue.getStatus().getName());
		try{
			switch (currentState) {
				case CREATED:	doImport();
								break;

				case IMPORTED:	runClassifier();
								break;

				case CLASSIFICATION_QUERIES:	logger.info ("Ticket " + issue.getKey() + " has classification queries.  Awaiting user action");
												break;

				case CLASSIFICATION_SUCCESS:	exportTask();
												break;

				case EXPORTED:	callSRS();
								break;

				case BUILT:		awaitRVFResults();
								break;

				case VALIDATED: logger.info ("Ticket " + issue.getKey() + " is awaiting user acceptance of validation.");
								break;

				case REJECTED:	revertImport();

				case ACCEPTED:	mergeTaskToMain();
								break;

				case PROMOTED:	versionMain();
								break;

				case FAILED:
				case PUBLISHED:
				case CLOSED:	logger.debug ("Ticket" + issue.getKey() + " is in final state " + currentState);
			}
		} catch (Exception e) {
			String errMsg = "Exception while processing ticket " + issue.getKey() + " at state " + currentState + ": " + e.getMessage();
			logger.error(errMsg, e);
			//Attempt to put the ticket into the failed State with a comment to that effect
			try {
				issue.addComment(errMsg);
				issue.transition().execute(State.FAILED.name());
			} catch (Exception e2) {
				logger.error("Additional exception while trying to record previous exception in Jira.", e2);
			}
		}
	}

	private void runClassifier() {
		throw new NotImplementedException("Code not yet written to runClassifier");
	}

	private void doImport() {
		throw new NotImplementedException("Code not yet written to doImport");
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
