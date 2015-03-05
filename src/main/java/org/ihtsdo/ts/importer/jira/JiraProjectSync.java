package org.ihtsdo.ts.importer.jira;

import net.rcarz.jiraclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JiraProjectSync {

	private final String projectKey;
	private final JiraClient jiraClient;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public JiraProjectSync(String projectKey, String jiraUrl, String jiraUsername, String jiraPassword) throws JiraSyncException {
		this.projectKey = projectKey;
		logger.info("Initialising Jira Project Sync for project key '{}'", this.projectKey);
		jiraClient = new JiraClient(jiraUrl, new BasicCredentials(jiraUsername, jiraPassword));
	}

	public List<String> listFieldValues(String fieldName) throws JiraException {
		List<String> fieldValues = new ArrayList<>();
		Issue.SearchResult searchResult = jiraClient.searchIssues("project = " + projectKey, fieldName);
		if (searchResult != null) {
			List<Issue> issues = searchResult.issues;
			for (Issue issue : issues) {
				fieldValues.add((String) issue.getField(fieldName));
			}
		}
		return fieldValues;
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
