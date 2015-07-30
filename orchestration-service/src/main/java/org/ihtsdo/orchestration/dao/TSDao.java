package org.ihtsdo.orchestration.dao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.orchestration.service.ValidationService.ValidationStatus;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonServiceException;

public class TSDao {
	
	private static final String statusFileName = "status.json";
	private static final String statusKey = "status";
	private static final String messageKey = "message";
	
	@Autowired
	private S3Client s3Client;

	private String tsReportBucketName;

	@Autowired
	public TSDao(final String tsReportBucketName) {
		this.tsReportBucketName = tsReportBucketName;
	}
	
	public void setStatus(String branchPath, String process, String status, String msg) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(statusKey, status);

		if (msg != null) {
			jsonObj.put(messageKey, msg);
		}

		InputStream is = new ByteArrayInputStream(jsonObj.toString().getBytes());
		s3Client.putObject(tsReportBucketName, getStatusFilePath(branchPath, process), is, null);
	}

	public String getStatus(String branchPath, String process) {
		String result = null;
		try {
			String statusString = s3Client.getString(tsReportBucketName, getStatusFilePath(branchPath, process));

			JSONObject jsonObj = new JSONObject(statusString);
			if (jsonObj.has(statusKey)) {
				result = jsonObj.getString(statusKey);
			}
		} catch (AmazonServiceException e) {
			// If the object isn't found, that's fine, we'll return null
			if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return null;
			} // Otherwise it's something more serious we want to know about
			throw e;
		}

		return result;
	}

	private String getStatusFilePath(String branchPath, String process) {
		return branchPath + "/" + process + "/" + statusFileName;
	}

}
