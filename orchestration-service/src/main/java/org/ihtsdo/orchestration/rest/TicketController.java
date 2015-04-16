package org.ihtsdo.orchestration.rest;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import org.ihtsdo.orchestration.clients.jira.JiraProjectSync;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.ts.importfilter.LoadException;
import org.ihtsdo.orchestration.workflow.TicketWorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/REST")
public class TicketController {

	private final Logger logger = LoggerFactory.getLogger(getClass());


	@Autowired
	private JiraProjectSync jiraService;

	@Autowired
	private TicketWorkflowManager twManager;

	@RequestMapping(value = "/{jiraProject}/tickets/{ticketId}", method = RequestMethod.GET)
	Issue getTicket(@PathVariable String jiraProject, @PathVariable String ticketId) throws IOException, LoadException, JiraException {
		logger.info("Looking for ticket " + ticketId);
		return jiraService.findIssue(ticketId);
	}

	@RequestMapping(value = "/{jiraProject}/tickets/process", method = RequestMethod.POST)
	Map<String, Object> processIncompleteTickets(@PathVariable String jiraProject) throws IOException, LoadException, JiraException {
		Map<String, Object> response = new HashMap();
		response.put("status", "Not yet implemented");
		return response;
	}

	// This method will be POST, but I'm leaving as GET for the moment so I can demo
	@RequestMapping(value = "/{workflowName}/tickets/{ticketId}/process", method = RequestMethod.GET)
	Map<String, Object> processIncompleteTicket(@PathVariable String workflowName, @PathVariable String ticketId) throws IOException,
			LoadException, JiraException, ResourceNotFoundException {
		return twManager.processTicket(workflowName, ticketId);
	}

}
