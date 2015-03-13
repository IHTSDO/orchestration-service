package org.ihtsdo.ts.workflow;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Status;

public class DailyDeltaTicketWorkflow extends TicketWorkflow {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public DailyDeltaTicketWorkflow() {
		workflowName = "DailyDelta";
	}

	protected void processTicket(Issue issue) {
		// Determine our current state and use state machine to work out what processing
		// should be undertaken in order to move to the next state
		Status currentState = issue.getStatus();
		logger.debug("Processing Ticket " + issue + " with current state: " + currentState);
	}


}
