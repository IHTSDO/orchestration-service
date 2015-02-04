package org.ihtsdo.ts.importer.snowowl;

import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import java.io.IOException;
import java.util.List;

public class SnowOwlRestClient {

	private final String snowOwlUrl;
	private final Resty resty;

	public SnowOwlRestClient(String snowOwlUrl, String username, String password) {
		this.snowOwlUrl = snowOwlUrl;
		this.resty = new Resty();
		resty.authenticate(snowOwlUrl, username, password.toCharArray());
	}

	@SuppressWarnings("unchecked")
	public List<String> listBranches() throws SnowOwlRestClientException {
		try {
			JSONResource tasks = resty.json(snowOwlUrl + "snomed-ct/MAIN/tasks");
			return (List<String>) tasks.get("items.taskId");
		} catch (IOException e) {
			throw new SnowOwlRestClientException("Failed to retrieve branch list.", e);
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Failed to parse branch list.", e);
		}
	}

}
