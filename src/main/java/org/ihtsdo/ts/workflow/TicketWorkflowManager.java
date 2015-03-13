package org.ihtsdo.ts.workflow;

import java.util.HashMap;
import java.util.Map;

import net.rcarz.jiraclient.JiraException;

import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketWorkflowManager {
	
	@Autowired
	DailyDeltaTicketWorkflow dd;

	//What types of workflow are known about?
	Map <String, TicketWorkflow> workflows;
	
	public void init() {
		workflows = new HashMap<String, TicketWorkflow>();
		workflows.put(dd.getName(), dd);
	}
	
	public void processIncompleteTickets() {
		
		//Loop through all the workflows we know, and tell each one to process Incomplete Tasks
		for (TicketWorkflow workflow : workflows.values()) {
			workflow.processIncompleteTickets();
		}
	}
	
	public Map<String, Object> processTicket(String workflowName, String ticketId) throws ResourceNotFoundException, JiraException {
		// I think we might want to specify the workflow in the REST API, which is why
		// I'm storing against strings rather than the concrete class itself.
		TicketWorkflow workflow = workflows.get(workflowName);
		if (workflow == null) {
			throw new ResourceNotFoundException("Could not find workflow: " + workflowName);
		}

		return workflow.processTicket(ticketId);
	}

}
