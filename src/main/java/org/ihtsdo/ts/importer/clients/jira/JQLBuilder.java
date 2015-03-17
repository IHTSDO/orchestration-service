package org.ihtsdo.ts.importer.clients.jira;


public class JQLBuilder {

	private final StringBuilder builder;

	public JQLBuilder() {
		builder = new StringBuilder();
	}

	public JQLBuilder project(String jiraProjectKey) {
		and();
		builder.append("project = ");
		builder.append(jiraProjectKey);
		return this;
	}

	public JQLBuilder statusNot(String status) {
		and();
		builder.append("status != ");
		builder.append(status);
		return this;
	}

	private void and() {
		if (builder.length() > 0) {
			builder.append(" AND ");
		}
	}

	@Override
	public String toString() {
		return builder.toString();
	}
}
