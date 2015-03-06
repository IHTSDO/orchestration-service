package org.ihtsdo.ts.importer.jira;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JiraProjectSync {

	public static final String TS_TASK_ID_FIELD = "customfield_11902";
	public static final String SUMMARY_FIELD = "summary";
	private final String projectKey;
	private final JiraClient jiraClient;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public JiraProjectSync(String projectKey, String jiraUrl, String jiraUsername, String jiraPassword) throws JiraSyncException {
		this.projectKey = projectKey;
		logger.debug("Initialising Jira Project Sync for project key '{}'", this.projectKey);
		jiraClient = new JiraClient(jiraUrl, new BasicCredentials(jiraUsername, jiraPassword));
	}

	public String createTask(String taskLabel) throws JiraException {
		Issue issue = jiraClient.createIssue(projectKey, "Task")
				.field(SUMMARY_FIELD, taskLabel)
				.execute();
		String key = issue.getKey();
		logger.info("Created issue '{}'", key);
		return key;
	}

	public boolean doesTaskExist(String taskLabel) throws JiraException {
		return findTask(taskLabel) != null;
	}

	public void addComment(String taskLabel, String commentString) throws JiraException {
		findTask(taskLabel).addComment(commentString);
	}

	private Issue findTask(String taskLabel) throws JiraException {
		List<Issue> issues = jiraClient.searchIssues("project = " + projectKey + " AND summary ~ '" + taskLabel + "'").issues;
		for (Issue issue : issues) {
			if (taskLabel.equals(issue.getSummary())) {
				return issue;
			}
		}
		return null;
	}
}
