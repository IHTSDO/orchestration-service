package org.ihtsdo.orchestration.workflow;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.orchestration.clients.jira.JiraProjectSync;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class TicketWorkflowManager {
	
	@Autowired
	private JiraProjectSync jiraProjectSync;

	private final Map <String, TicketWorkflow> workflows;

	private final Map<String, String> issueStatuses;

	private final Map<String, String> skippedTickets;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public TicketWorkflowManager(Map<String, TicketWorkflow> workflows) {
		this.workflows = workflows;
		issueStatuses = new HashMap<>();
		skippedTickets = new HashMap<>();
		String workflowsStr = "";
		for (String workflowName : workflows.keySet()) {
			TicketWorkflow workflow = workflows.get(workflowName);
			workflowsStr += " " + workflowName + "[" + workflow.getProjectKey() + "]";
		}
		logger.info("TicketWorkflowManager configured to examine workflows:" + workflowsStr);
	}

	public void processIncompleteTickets() {
		for (String workflowName : workflows.keySet()) {
			TicketWorkflow workflow = workflows.get(workflowName);
			String jqlSelectStatement = workflow.getInterestingTicketJQLSelectStatement();
			try {
				// This list should be returned so we process in time ascending order
				List<Issue> issues = jiraProjectSync.findIssues(jqlSelectStatement);
				sortIssuesOldestFirst(issues);
				for (Issue issue : issues) {
					while (!workflow.isComplete(issue) && hasStatusChanged(issue)) {
						processChangedTicket(workflow, issue);
					}

					// If the ticket is still incomplete, do not process any further tickets for this workflow
					if (!workflow.isComplete(issue)) {
						if (!alreadyReported(workflowName, issue)) {
							logger.warn("Ticket {} is incomplete. Skipping any further tickets for workflow {}.", issue.getKey(),
									workflowName);
						}
						break;
					}
				}
			} catch (JiraException e) {
				logger.error("Failed to find issues for workflow '{}'", workflowName, e);
			}
		}
	}

	protected void sortIssuesOldestFirst(List<Issue> issues) {
		Collections.sort(issues, new Comparator<Issue>() {
			@Override
			public int compare(Issue o1, Issue o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
	}

	/*
	 * Only log that we're skipping a ticket once for each workflow and issue state
	 */
	private boolean alreadyReported(String workflowName, Issue issue) {
		// What was the last ticket + state we skipped for this workflow?
		boolean alreadyReported = false;
		String lastIssueState = skippedTickets.get(workflowName);
		String thisIssueState = issue.getKey() + issue.getStatus().getName();

		if (lastIssueState != null && lastIssueState.equals(thisIssueState)) {
			alreadyReported = true;
		}
		skippedTickets.put(workflowName, thisIssueState);

		return alreadyReported;
	}

	public Map<String, Object> processTicket(String workflowName, String ticketId) throws ResourceNotFoundException, JiraException {
		// I think we might want to specify the workflow in the REST API, which is why
		// I'm storing against strings rather than the concrete class itself.
		TicketWorkflow workflow = workflows.get(workflowName);
		if (workflow == null) {
			throw new ResourceNotFoundException("Could not find workflow: " + workflowName);
		}
		Issue issue = jiraProjectSync.findIssue(ticketId);
		if (issue == null) {
			throw new ResourceNotFoundException("Could not find ticket: " + ticketId);
		}

		return processChangedTicket(workflow, issue);
	}

	private Map<String, Object> processChangedTicket(TicketWorkflow workflow, Issue issue) {
		String initialStatus = issue.getStatus().getName();
		logger.debug("Processing Ticket {} with current state '{}'", issue.getKey(), initialStatus);

		Map<String, Object> ticketInfo = new HashMap<>();
		ticketInfo.put("Ticket Id", issue.getKey());
		ticketInfo.put("Initial Status", initialStatus);
		workflow.processChangedTicket(issue);
		ticketInfo.put("Current Status", issue.getStatus().getName());
		return ticketInfo;
	}

	synchronized private boolean hasStatusChanged(Issue issue) {
		String key = issue.getKey();
		String currentStatus = issue.getStatus().getName();
		String oldStatus = issueStatuses.get(key);
		if (oldStatus == null || !oldStatus.equals(currentStatus)) {
			// record new status
			issueStatuses.put(key, currentStatus);
			return true;
		} else {
			return false;
		}
	}

}
