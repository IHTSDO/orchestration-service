package org.ihtsdo.ts.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import org.ihtsdo.ts.importer.JiraTransitions;
import org.ihtsdo.ts.importer.clients.jira.JQLBuilder;
import org.ihtsdo.ts.importer.clients.snowowl.SnowOwlRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

@Resource
public class DailyExportTicketWorkflow extends TSAbstractTicketWorkflow implements TicketWorkflow {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String interestingTicketsJQL;
	
	private final String jiraProjectKey;

	@Autowired
	public DailyExportTicketWorkflow(String jiraDailyExportProjectKey) {
		this.jiraProjectKey = jiraDailyExportProjectKey;
		interestingTicketsJQL = new JQLBuilder()
				.project(jiraDailyExportProjectKey)
				.statusNot(jiraState(DEState.VALIDATION_ACCEPTED))
				.statusNot(DEState.FAILED.toString())
				.statusNot(DEState.CLOSED.toString())
				.toString();
	}

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	synchronized public void processChangedTicket(Issue issue) {
		DEState currentState = (DEState) getState(issue);
		try {
			switch (currentState) {
				case CREATED:
					runClassifier(issue, SnowOwlRestClient.BranchType.MAIN);
					break;

				case CLASSIFIED_WITH_QUERIES:
					logger.info("Ticket {} has classification queries.  Awaiting user action", issue.getKey());
					break;

				case CLASSIFICATION_ACCEPTED:
					saveClassification(issue, SnowOwlRestClient.BranchType.MAIN);
					break;

				case CLASSIFIED_SUCCESSFULLY:
					export(issue, SnowOwlRestClient.BranchType.MAIN);
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
				case FAILED:
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
				jiraProjectSync.addComment(issue, errMsg);
				jiraProjectSync.updateStatus(issue, JiraTransitions.FAILED);
			} catch (JiraException e2) {
				logger.error("Additional exception while trying to record previous exception in Jira.", e2);
			}
		}
	}

	
	private static enum DEState {
		CREATED,
		CLASSIFIED_WITH_QUERIES,
		CLASSIFICATION_ACCEPTED,
		CLASSIFIED_SUCCESSFULLY,
		EXPORTED,
		BUILT,
		VALIDATED,
		VALIDATION_ACCEPTED,
		FAILED,
		CLOSED
	}

	private static DEState[] COMPLETE_STATES = { DEState.VALIDATION_ACCEPTED, DEState.FAILED, DEState.CLOSED };


	@Override
	protected Enum[] getCompleteStates() {
		return COMPLETE_STATES;
	}

	@Override
	protected Enum getState(Issue issue) {
		return DEState.valueOf(issue.getStatus().getName().toUpperCase().replace(" ", "_"));
	}

	@Override
	public String getProjectKey() {
		return jiraProjectKey;
	}

}
