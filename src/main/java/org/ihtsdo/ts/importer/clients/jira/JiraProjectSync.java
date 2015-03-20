package org.ihtsdo.ts.importer.clients.jira;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class JiraProjectSync {

	public static final String SUMMARY_FIELD = "summary";
	private final String projectKey;
	private final JiraClient jiraClient;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static final String NEW_LINE = "\n";

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

	public void addComment(String taskKey, String commentString) throws JiraException {
		logger.info("Adding comment to '{}': [{}]", taskKey, commentString);
		findIssue(taskKey).addComment(commentString);
	}

	public void updateStatus(String taskKey, String statusName) throws JiraException {
		Issue issue = findIssue(taskKey);
		issue.transition().execute(statusName);
	}

	public Issue findIssue(String taskKey) throws JiraException {
		return jiraClient.getIssue(taskKey);
	}

	/**
	 * Note - will only return the first page of issues. In our case this is enough.
	 * @param jqlSelectStatement
	 * @return
	 * @throws JiraException
	 */
	public List<Issue> findIssues(String jqlSelectStatement) throws JiraException {
		return jiraClient.searchIssues(jqlSelectStatement, "*all").issues;
	}

	public void addComment(String issueKey, String introduction, Map<String, String> itemMap) throws JiraException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(introduction);
		for (Map.Entry<String, String> item : itemMap.entrySet()) {
			buffer.append(NEW_LINE).append(item.getKey()).append(": ").append(item.getValue());
		}
		addComment(issueKey, buffer.toString());
	}
}
