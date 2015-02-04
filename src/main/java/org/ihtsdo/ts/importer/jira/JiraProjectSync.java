package org.ihtsdo.ts.importer.jira;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraProjectSync {

	private final String projectKey;
	private final JiraClient jiraClient;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public JiraProjectSync(String projectKey, String jiraUrl, String jiraUsername, String jiraPassword) throws JiraSyncException {
		this.projectKey = projectKey;
		logger.info("Initialising Jira Project Sync for project key '{}'", this.projectKey);
		jiraClient = new JiraClient(jiraUrl, new BasicCredentials(jiraUsername, jiraPassword));
	}

	public void assertProjectExists() throws JiraSyncException {
		try {
			Project project = jiraClient.getProject(projectKey);
			logger.info("Found project named '{}'", project.getName());
		} catch (JiraException e) {
			throw new JiraSyncException("Failed to assert that project with key '" + projectKey + "' exists.", e);
		}
	}

}
