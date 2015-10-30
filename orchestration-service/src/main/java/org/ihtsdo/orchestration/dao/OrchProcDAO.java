package org.ihtsdo.orchestration.dao;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.httpclient.HttpStatus;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.utils.DateUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class OrchProcDAO {

	private static final String STATUS_FILE_NAME = "status.json";
	private static final String REPORT_FILE_NAME = "report.json";
	private static final String STATUS_KEY = "status";
	private static final String MESSAGE_KEY = "message";
	public static final String VALIDATION = "validation";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private S3Client s3Client;

	private String tsReportBucketName;

	@Autowired
	public OrchProcDAO(final String tsReportBucketName) {
		this.tsReportBucketName = tsReportBucketName;
	}
	
	public void setStatus(String branchPath, String process, String status, String msg) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(STATUS_KEY, status);

		if (msg != null) {
			jsonObj.put(MESSAGE_KEY, msg);
		}

		InputStream is = new ByteArrayInputStream(jsonObj.toString().getBytes());
		s3Client.putObject(tsReportBucketName, getStatusFilePath(branchPath, process), is, null);
		logger.info("Set {} status to {} for branchPath {}.", process, status, branchPath);
	}

	public String getStatus(String branchPath, String process) {
		String result = null;
		try {
			String statusString = s3Client.getString(tsReportBucketName, getStatusFilePath(branchPath, process));

			JSONObject jsonObj = new JSONObject(statusString);
			if (jsonObj.has(STATUS_KEY)) {
				result = jsonObj.getString(STATUS_KEY);
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
		return branchPath + "/" + process + "/" + STATUS_FILE_NAME;
	}

	private String getNewReportFilePath(String branchPath, String process) {
		String dt = DateUtils.now(DateUtils.YYYYMMDD_HHMMSS);
		return branchPath + "/" + process + "/" + dt + "/" + REPORT_FILE_NAME;
	}

	public void saveReport(String branchPath, String process, JSONObject rvfReport) {
		InputStream is = new ByteArrayInputStream(rvfReport.toString().getBytes());
		s3Client.putObject(tsReportBucketName, getNewReportFilePath(branchPath, process), is, null);
	}

	public String getLatestValidationReport(String path) throws IOException {
		path = removeTrailingSlash(path);
		final List<String> reports = listValidationReportsForPath(path);
		if (!reports.isEmpty()) {
			return getReport(path + "/" + VALIDATION + "/" + reports.get(reports.size() - 1));
		}
		return null;
	}

	public List<String> listValidationReportsForPath(String path) {
		List<String> reports = new ArrayList<>();
		path = removeTrailingSlash(path);
		final ObjectListing objectListing = s3Client.listObjects(tsReportBucketName, path + "/" + VALIDATION);
		for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
			final String key = s3ObjectSummary.getKey();
			if (key.endsWith(REPORT_FILE_NAME)) {
				final String[] pathParts = key.split("\\/");
				reports.add(pathParts[pathParts.length - 2]);
			}
		}
		return reports;
	}

	private String removeTrailingSlash(String path) {
		if (path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	public String getReport(String reportPath) throws IOException {
		final S3Object object = s3Client.getObject(tsReportBucketName, reportPath + "/" + REPORT_FILE_NAME);
		try (InputStream in = object.getObjectContent()) {
			return StreamUtils.copyToString(in, Charset.forName("UTF-8"));
		}
	}
}
