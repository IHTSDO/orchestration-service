package org.ihtsdo.orchestration.clients.snowowl;

import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClient;
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

	public SnowOwlRestClient getClient(String authToken) {
		SnowOwlRestClient snowOwlRestClient = new SnowOwlRestClient(url, authToken);
		snowOwlRestClient.setReasonerId(reasonerId);
		snowOwlRestClient.setImportTimeoutMinutes(importTimeout);
		snowOwlRestClient.setClassificationTimeoutMinutes(classificationTimeout);
		return snowOwlRestClient;
	}

}
