package org.ihtsdo.ts.importer.clients.jira;

import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraDataHelper {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private String jiraDataMarker;

	public JiraDataHelper(String jiraDataMarker) {
		this.jiraDataMarker = jiraDataMarker;
	}

	public void putData(Issue issue, String key, String value) throws JiraException {
		logger.debug("Writing data item '{}' to ticket {} with key {}", value, issue.getKey(), key);
		issue.addComment(getWorkflowDataId(key) + value);
	}

	public String getData(Issue issue, String key) throws JiraException {
		String targetWorkflowDataId = getWorkflowDataId(key);
		// Update our issue with Jira to pick up any new comments;
		issue.refresh();
		List<Comment> comments = issue.getComments();
		for (Comment thisComment : comments) {
			String commentText = thisComment.getBody();
			if (commentText.startsWith(targetWorkflowDataId)) {
				return commentText.substring(targetWorkflowDataId.length());
			}
		}
		return null;
	}

	private String getWorkflowDataId(final String key) {
		return jiraDataMarker + key + ": ";
	}

}
