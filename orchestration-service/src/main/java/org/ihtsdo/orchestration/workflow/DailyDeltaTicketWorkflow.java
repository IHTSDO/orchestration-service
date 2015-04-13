package org.ihtsdo.orchestration.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import org.ihtsdo.orchestration.importer.Importer;
import org.ihtsdo.orchestration.clients.jira.JQLBuilder;
import org.ihtsdo.orchestration.clients.jira.JiraDataHelper;
import org.ihtsdo.orchestration.clients.jira.JiraSyncException;
import org.ihtsdo.orchestration.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importfilter.ImportFilterService;
import org.ihtsdo.ts.importfilter.ImportFilterServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.Resource;

@Resource
public class DailyDeltaTicketWorkflow extends TSAbstractTicketWorkflow implements TicketWorkflow {

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private JiraDataHelper jiraDataHelper;

	private final String interestingTicketsJQL;

	private final String jiraProjectKey;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	public DailyDeltaTicketWorkflow(String jiraProjectKey) {
		this.jiraProjectKey = jiraProjectKey;
		interestingTicketsJQL = new JQLBuilder()
				.project(jiraProjectKey)
				.statusNot(DDState.CREATED.toString())
				.statusNot(DDState.FAILED.toString())
				.statusNot(DDState.PROMOTED.toString())
				.statusNot(DDState.CLOSED.toString())
				.toString();
	}

	@Override
	public String getInterestingTicketJQLSelectStatement() {
		return interestingTicketsJQL;
	}

	@Override
	synchronized public void processChangedTicket(Issue issue) {
		DDState currentState = getState(issue);
		try {
			switch (currentState) {
				case CREATED:
					logger.info("DailyDelta Workflow ignoring ticket at status CREATED: {}", issue.getKey());
					break;
				case IMPORTED:
					runClassifier(issue, SnowOwlRestClient.BranchType.BRANCH);
					break;

				case CLASSIFIED_WITH_QUERIES:
					logger.info("Ticket {} has classification queries.  Awaiting user action", issue.getKey());
					break;

				case CLASSIFICATION_ACCEPTED:
					saveClassification(issue, SnowOwlRestClient.BranchType.BRANCH);
					break;

				case CONTENT_REJECTED:
					revertImport(issue);
					break;

				case CLASSIFIED_SUCCESSFULLY:
					export(issue, SnowOwlRestClient.BranchType.BRANCH);
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
				jiraProjectSync.updateStatus(issue, JiraTransitions.FAILED);
			} catch (JiraException | JiraSyncException e2) {
				logger.error("Additional exception while trying to record previous exception in Jira.", e2);
			}
		}
	}

	protected DDState getState(Issue issue) {
		return DDState.valueOf(issue.getStatus().getName().toUpperCase().replace(" ", "_"));
	}

	private void revertImport(Issue issue) throws ImportFilterServiceException, JiraSyncException, JiraException {
		String selectedArchiveVersion = jiraDataHelper.getData(issue, Importer.SELECTED_ARCHIVE_VERSION);
		Assert.notNull(selectedArchiveVersion, "Selected archive version can not be null.");
		importFilterService.putSelectionArchiveBackInBacklog(selectedArchiveVersion);
		jiraProjectSync.updateStatus(issue, TRANSITION_TO_CLOSED);
	}

	private enum DDState {
		CREATED,
		IMPORTED,
		CLASSIFIED_WITH_QUERIES,
		CLASSIFICATION_ACCEPTED,
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

	private static DDState[] COMPLETE_STATES = { DDState.PROMOTED, DDState.FAILED, DDState.CLOSED };

	@Override
	protected Enum[] getCompleteStates() {
		return COMPLETE_STATES;
	}

	@Override
	public String getProjectKey() {
		return jiraProjectKey;
	}

}
