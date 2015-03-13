package org.ihtsdo.ts.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Status;

public abstract class TicketWorkflow {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JiraProjectSync jiraService;

	protected String workflowName;

	public String getName() {
		return workflowName;
	}

	protected List<Issue> findIncompleteTickets() {
		// We'll have to find tickets that belong to projects that implement the specific
		// workflow. Or maintain a list of those projects.
		throw new NotImplementedException();
	}

	public void processIncompleteTickets() {
		List<Issue> incompleteTickets = findIncompleteTickets();
		for (Issue issue : incompleteTickets) {
			processTicket(issue);
		}
	}

	public Map<String, Object> processTicket(String ticketId) throws JiraException {
		Issue ticket = jiraService.findIssue(ticketId);
		return processTicket(ticket);
	}

	protected Map<String, Object> processTicket(Issue issue) {
		// Determine our current state and use state machine to work out what processing
		// should be undertaken in order to move to the next state
		Status currentState = issue.getStatus();
		logger.debug("Processing Ticket " + issue + " with current state: " + currentState);
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("Ticket Id", issue.getKey());
		results.put("Current Status", currentState.getName());
		doStateMachine(issue);
		return results;
	}

	protected abstract void doStateMachine(Issue issue);

}
