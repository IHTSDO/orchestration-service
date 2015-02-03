package org.ihtsdo.ts.importer;

import net.rcarz.jiraclient.*;

import java.io.IOException;
import java.util.Properties;

public class App {

	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		properties.load(App.class.getResourceAsStream("/jira.properties"));

		BasicCredentials basicCredentials = new BasicCredentials(properties.getProperty("jira.username"), properties.getProperty("jira.password"));
		JiraClient jira = new JiraClient(properties.getProperty("jira.url"), basicCredentials);
		try {

			Project project = jira.getProject("TID");
			System.out.println(project.getName());
			System.out.println(project.getId());

//			jira.createIssue("TID", "Task").field(Field.SUMMARY, "New Concept A").execute();

//			jira.searchIssues("");

//			Issue issue = jira.getIssue("CDT-684");
//			System.out.println(issue);
//			System.out.println(issue.getStatus());
//			List<Comment> comments = issue.getComments();
//			for (Comment comment : comments) {
//				System.out.println(comment.getBody());
//			}
		} catch (JiraException e) {
			e.printStackTrace();
		}
	}

}
