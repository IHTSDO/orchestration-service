package org.ihtsdo.ts.workflow;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Status;

@Resource
public class DailyDeltaTicketWorkflow extends TicketWorkflow {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static enum State {
		CREATED,
		IMPORTED,
		CLASSIFICATION_SUCCESS,
		CLASSIFICATION_QUERIES,
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

	protected Map <String, Object> processTicket(Issue issue) {
		// Determine our current state and use state machine to work out what processing
		// should be undertaken in order to move to the next state
		Status currentState = issue.getStatus();
		logger.debug("Processing Ticket " + issue + " with current state: " + currentState);
		Map <String, Object> results = new HashMap<String, Object>();
		results.put("Ticket Id", issue.getKey());
		results.put("Current Status", currentState.getName());
		return results;
	}


}
