package org.ihtsdo.orchestration.schedule;

import org.ihtsdo.orchestration.workflow.TicketWorkflowManager;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckJiraTickets implements Runnable {

	@Autowired
	private TicketWorkflowManager ticketWorkflowManager;

	@Override
	public void run() {
		// Ticket Workflow not currently required.
		// ticketWorkflowManager.processIncompleteTickets();
	}

}
