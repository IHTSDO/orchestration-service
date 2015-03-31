package org.ihtsdo.rvf.client;


import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import us.monoid.web.JSONResource;
import us.monoid.web.RestyMod;

public class RVFRestClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String JSON_FIELD_STATUS = "Status";
	public static enum RVF_STATE { RUNNING, COMPLETE, FAILED, UNKNOWN };

	protected static final String CONTENT_TYPE_ANY = "*/*";
	protected static final String CONTENT_TYPE_XML = "text/xml";
	protected static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
	protected static final String CONTENT_TYPE_TEXT = "text/plain;charset=UTF-8";

	private final RestyMod resty;
	private final int pollPeriod;
	private final int maxElapsedTime;
	private final int INDENT = 2;

	/**
	 * 
	 * @param pollPeriod
	 *            time between each check of the URL in seconds
	 * @param timeout
	 *            time to continue polling for in minutes
	 */
	public RVFRestClient(int pollPeriod, int timeout) {
		this.resty = new RestyMod();
		this.pollPeriod = pollPeriod * 1000;
		maxElapsedTime = timeout * 60 * 1000;
	}

	public void waitForResults(String pollURL) throws Exception {
		
		//Poll the URL and see what status the results are in
		boolean isFinalState = false;
		long msElapsed = 0;

		Assert.notNull(pollURL, "Unable to check for RVF results - location not known.");

		if (!pollURL.startsWith("http")) {
			throw new ProcessingException("RVF location not available to check.  Instead we have: " + pollURL);
		}

		while (!isFinalState) {
			JSONResource json = resty.json(pollURL);
			Object responseState = json.get(JSON_FIELD_STATUS);
			RVF_STATE currentState = RVF_STATE.UNKNOWN;
			try {
				currentState = RVF_STATE.valueOf(responseState.toString());
			} catch (Exception e) {
				throw new ProcessingException ("Failed to determine RVF Status from response: " + json.object().toString(INDENT));
			}
			
			logger.debug("RVF Reported state: {}", currentState);

			switch (currentState){
				case RUNNING:
					Thread.sleep(pollPeriod);
				msElapsed += pollPeriod;
					break;
				case FAILED:
					throw new ProcessingException("RVF reported a technical failure: " + json.object().toString(INDENT));
				case COMPLETE:
					isFinalState = true;
					break;
				default:
					throw new ProcessingException("RVF Reponse was not recognised: " + currentState);
			}

			if (msElapsed > maxElapsedTime) {
				throw new ProcessingException("RVF did not complete within the allotted time ");
			}
		}
	}
	

}
