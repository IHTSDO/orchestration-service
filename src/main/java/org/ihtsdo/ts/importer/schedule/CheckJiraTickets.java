package org.ihtsdo.ts.importer.schedule;

import org.ihtsdo.ts.workflow.TicketWorkflowManager;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckJiraTickets implements Runnable {

	@Autowired
	private TicketWorkflowManager ticketWorkflowManager;

	@Override
	public void run() {
		ticketWorkflowManager.processIncompleteTickets();
	}

}
