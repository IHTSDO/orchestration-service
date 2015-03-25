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
	public static final String PRIORITY_FIELD = "priority";
	public static final String PRIORITY_CRITICAL = "Critical";
	private static final String NEW_LINE = "\n";

	private final String projectKey;
	private final String jiraUrl;
	private final String jiraUsername;
	private final String jiraPassword;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public JiraProjectSync(String projectKey, String jiraUrl, String jiraUsername, String jiraPassword) throws JiraSyncException {
		this.projectKey = projectKey;
		this.jiraUrl = jiraUrl;
		this.jiraUsername = jiraUsername;
		this.jiraPassword = jiraPassword;
	}

	public String createTask(String taskLabel) throws JiraException {
		Issue issue = getJiraClient().createIssue(projectKey, "Task")
				.field(SUMMARY_FIELD, taskLabel)
				.field(PRIORITY_FIELD, PRIORITY_CRITICAL)
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
		return getJiraClient().getIssue(taskKey);
	}

	/**
	 * Note - will only return the first page of issues. In our case this is enough.
	 * @param jqlSelectStatement
	 * @return
	 * @throws JiraException
	 */
	public List<Issue> findIssues(String jqlSelectStatement) throws JiraException {
		return getJiraClient().searchIssues(jqlSelectStatement, "*all").issues;
	}

	public void addComment(String issueKey, String introduction, Map<String, String> itemMap) throws JiraException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(introduction);
		for (Map.Entry<String, String> item : itemMap.entrySet()) {
			buffer.append(NEW_LINE).append(item.getKey()).append(": ").append(item.getValue());
		}
		addComment(issueKey, buffer.toString());
	}

	private JiraClient getJiraClient() {
		return new JiraClient(this.jiraUrl, new BasicCredentials(this.jiraUsername, this.jiraPassword));
	}
}
