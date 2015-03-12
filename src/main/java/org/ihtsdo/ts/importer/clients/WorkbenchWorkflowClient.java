package org.ihtsdo.ts.importer.clients;

import java.util.HashSet;
import java.util.Set;

public class WorkbenchWorkflowClient {

	public Set<Long> getCompletedConceptSctids() {
		Set<Long> set = new HashSet<>();
		set.add(504009L);
		return set;
	}

}
