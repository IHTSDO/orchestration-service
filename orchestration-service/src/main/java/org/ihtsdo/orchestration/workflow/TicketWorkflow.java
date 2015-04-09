package org.ihtsdo.orchestration.workflow;

import net.rcarz.jiraclient.Issue;

public interface TicketWorkflow {
	
	public String getInterestingTicketJQLSelectStatement();

	public void processChangedTicket(Issue issue);

	public boolean isComplete(Issue issue);

	public String getProjectKey();

}
