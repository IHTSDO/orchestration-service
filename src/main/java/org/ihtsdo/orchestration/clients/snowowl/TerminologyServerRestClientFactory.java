package org.ihtsdo.orchestration.clients.snowowl;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TerminologyServerRestClientFactory {

	@Value("${snowowl.url}")
	private String url;

	@Value("${snowowl.reasonerId}")
	private String reasonerId;

	@Value("${snowowl.import.timeout}")
	private int importTimeout;

	@Value("${snowowl.classification.timeout}")
	private int classificationTimeout;

	public SnowstormRestClient getClient(String authToken) {
		SnowstormRestClient snowstormRestClient = new SnowstormRestClient(url, authToken);
		snowstormRestClient.setReasonerId(reasonerId);
		snowstormRestClient.setImportTimeoutMinutes(importTimeout);
		snowstormRestClient.setClassificationTimeoutMinutes(classificationTimeout);
		return snowstormRestClient;
	}

}
