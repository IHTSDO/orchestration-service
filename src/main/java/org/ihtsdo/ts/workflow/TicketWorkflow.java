package org.ihtsdo.ts.workflow;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.ihtsdo.ts.importer.clients.jira.JiraProjectSync;
import org.springframework.beans.factory.annotation.Autowired;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

public abstract class TicketWorkflow {
	
	@Autowired
	private JiraProjectSync jiraService;

	protected String workflowName;

	public String getName() {
		return workflowName;
	}

	protected abstract void processTicket(Issue issue);

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

	public void processTicket(String ticketId) throws JiraException {
		Issue ticket = jiraService.findIssue(ticketId);
		processTicket(ticket);
	}

}
