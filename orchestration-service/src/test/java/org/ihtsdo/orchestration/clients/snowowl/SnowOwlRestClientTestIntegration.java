package org.ihtsdo.orchestration.clients.snowowl;

import org.ihtsdo.orchestration.Config;
import org.ihtsdo.orchestration.TestProperties;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Config.class, TestProperties.class})
public class SnowOwlRestClientTestIntegration {

	@Autowired
	private SnowOwlRestClient client;

	private List<String> taskBranchesToDelete;
	public String runName;

	private static final String INTEGRATION_TESTING = "integration_testing";

	@Before
	public void setup() throws RestClientException {
		runName = "run_" + SnowOwlRestClient.SIMPLE_DATE_FORMAT.format(new Date());
		taskBranchesToDelete = new ArrayList<>();
	}

	@Test
	public void testCreateProjectAndTask() throws RestClientException {
		client.createProjectBranchIfNeeded(INTEGRATION_TESTING);

		// Check it's in the listing
		List<String> projectNames = client.listProjectBranches();
		Assert.assertTrue(projectNames.contains(INTEGRATION_TESTING));

		// Create a project task
		String task1Name = runName + "task-1";
		client.createProjectTask(INTEGRATION_TESTING, task1Name);
		taskBranchesToDelete.add(task1Name);

		List<String> tasks = client.listProjectTasks(INTEGRATION_TESTING);
		Assert.assertTrue(tasks.contains(task1Name));
	}

	@After
	public void after() throws RestClientException {
		for (String branchToDelete : taskBranchesToDelete) {
			client.deleteTaskBranch(INTEGRATION_TESTING, branchToDelete);
		}
	}

}
