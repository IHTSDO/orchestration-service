package org.ihtsdo.orchestration.clients.rvf;


import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.orchestration.clients.srs.SRSFileDAO;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.otf.rest.client.resty.HttpEntityContent;
import org.ihtsdo.otf.rest.client.resty.RestyServiceHelper;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import us.monoid.web.JSONResource;
import us.monoid.web.RestyMod;

public class RVFRestClient {

	private static final String RVF_TS = "RVF_TS";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String JSON_FIELD_STATUS = "status";

	public enum RVF_STATE {
		QUEUED, RUNNING, COMPLETE, FAILED, UNKNOWN
	};

	private final RestyMod resty;
	private final int pollPeriod;
	private final int maxElapsedTime;
	private final int INDENT = 2;
	
	@Autowired
	protected SRSFileDAO srsDAO;

	private String rvfRootUrl;
	

	/**
	 * 
	 * @param pollPeriod
	 *            time between each check of the URL in seconds
	 * @param timeout
	 *            time to continue polling for in minutes
	 */
	public RVFRestClient(String rvfRootUrl,int pollPeriod, int timeout) {
		this.resty = new RestyMod();
		this.rvfRootUrl = rvfRootUrl;
		this.pollPeriod = pollPeriod * 1000;
		maxElapsedTime = timeout * 60 * 1000;
	}
	
	public JSONObject waitForResponse(String pollURL) throws Exception {
		JSONResource resource = waitForResults(pollURL);
		if (resource != null) {
			return new JSONObject(resource.object().toString(2));
		} else {
			return new JSONObject();
		}
	}

	public JSONResource waitForResults(String pollURL) throws Exception  {
		
		//Poll the URL and see what status the results are in
		boolean isFinalState = false;
		long msElapsed = 0;

		Assert.notNull(pollURL, "Unable to check for RVF results - location not known.");

		if (!pollURL.startsWith("http")) {
			throw new ProcessingException("RVF location not available to check.  Instead we have: " + pollURL);
		}

		int pollCount = 0;
		RVF_STATE lastState = RVF_STATE.UNKNOWN;
		JSONResource json = null;
		
		while (!isFinalState) {
			json = resty.json(pollURL);
			Object responseState = json.get(JSON_FIELD_STATUS);
			RVF_STATE currentState;

			try {
				currentState = RVF_STATE.valueOf(responseState.toString());
			} catch (Exception e) {
				throw new ProcessingException ("Failed to determine RVF Status from response: " + json.object().toString(INDENT));
			}
			
			// We'll just report every 10th state and when the state changes to prevent excessive logging
			if (logger.isDebugEnabled() && (pollCount % 10 == 0 || currentState != lastState)) {
				logger.debug("RVF Reported state: {}", currentState);
			}

			switch (currentState){
				case QUEUED:
				case RUNNING:
					Thread.sleep(pollPeriod);
					msElapsed += pollPeriod;
					pollCount++;
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

			lastState = currentState;
		}
		return json;

	}

	public File prepareExportFilesForValidation(File exportArchive, SRSProjectConfiguration config, boolean includeExternalFiles) throws ProcessWorkflowException, IOException {
		File extractDir= srsDAO.extractAndConvertExportWithRF2FileNameFormat(exportArchive, config.getReleaseDate(), includeExternalFiles);
		File zipFile = File.createTempFile(config.getProductName() + "_" + config.getReleaseDate(), ".zip");
		logger.debug("zip updated file into:" + zipFile);
		ZipFileUtils.zip(extractDir.getAbsolutePath(), zipFile.getAbsolutePath());
		return zipFile;
	}

	public String runValidationForRF2DeltaExport(File zipFile, SRSProjectConfiguration config) throws ProcessingException {
		String rvfResultUrl = null;
		String validaitonUrl = rvfRootUrl + "/run-post";
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		multipartEntityBuilder.addBinaryBody("file", zipFile, ContentType.create("multipart/form-data"), zipFile.getName());
		multipartEntityBuilder.addTextBody("rf2DeltaOnly", Boolean.TRUE.toString());
		multipartEntityBuilder.addTextBody("previousIntReleaseVersion", config.getPreviousInternationalRelease());
		multipartEntityBuilder.addTextBody("groups", config.getAssertionGroupNames());
		multipartEntityBuilder.addTextBody("runId", Long.toString(System.currentTimeMillis()));
		multipartEntityBuilder.addTextBody("failureExportMax", config.getFailureExportMax());
		multipartEntityBuilder.addTextBody("storageLocation", RVF_TS);
		multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		HttpEntity httpEntity = multipartEntityBuilder.build();
		JSONResource response;
		try {
			response = resty.json(validaitonUrl, new HttpEntityContent(httpEntity));
			RestyServiceHelper.ensureSuccessfull(response);
			rvfResultUrl = response.get("resultURL").toString();
		} catch ( Exception e) {
			throw new ProcessingException("Failed to upload " + zipFile.getName() + " to RVF for validation", e);
		}
		return rvfResultUrl;
	}

}
