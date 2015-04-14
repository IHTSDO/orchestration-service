package org.ihtsdo.orchestration.clients.jira;

import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JiraDataHelper {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private String jiraDataMarker;

	public JiraDataHelper(String jiraDataMarker) {
		this.jiraDataMarker = jiraDataMarker;
	}

	public void putData(Issue issue, String key, String value) throws JiraException {
		logger.debug("Writing data item '{}' to ticket {} with key '{}'", value, issue.getKey(), key);
		issue.addComment(getWorkflowDataId(key) + value);
	}

	public String getLatestData(Issue issue, String key) throws JiraException {
		String targetWorkflowDataId = getWorkflowDataId(key);
		// Update our issue with Jira to pick up any new comments;
		issue.refresh();
		List<Comment> comments = new ArrayList<>(issue.getComments());
		Collections.reverse(comments);
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
