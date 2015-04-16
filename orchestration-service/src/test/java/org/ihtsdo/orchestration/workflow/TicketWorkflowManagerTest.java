package org.ihtsdo.orchestration.workflow;

import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TicketWorkflowManagerTest {

	private TicketWorkflowManager ticketWorkflowManager;

	@Before
	public void setUp() throws Exception {
		ticketWorkflowManager = new TicketWorkflowManager(new HashMap<String, TicketWorkflow>());
	}

	@Test
	public void testSortIssuesOldestFirst() throws Exception {
		List<Issue> issues = new ArrayList<>();
		issues.add(createIssue("ABC-2"));
		issues.add(createIssue("ABC-3"));
		issues.add(createIssue("ABC-1"));

		Assert.assertEquals("[ABC-2, ABC-3, ABC-1]", issues.toString());
		ticketWorkflowManager.sortIssuesOldestFirst(issues);
		Assert.assertEquals("[ABC-1, ABC-2, ABC-3]", issues.toString());
	}

	private Issue createIssue(String id) {
		JSONObject map = new JSONObject();
		map.put("key", id);
		map.put("fields", new HashMap<>());
		return Field.getResource(Issue.class, map, null);
	}
}
