package org.ihtsdo.ts.importer.clients.snowowl;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.ts.importer.clients.resty.HttpEntityContent;
import org.ihtsdo.ts.importer.clients.resty.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.RestyMod;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class SnowOwlRestClient {

	public static final int IMPORT_TIMEOUT_MINUTES = 30;
	public static final int CLASSIFICATION_TIMEOUT_MINUTES = 10;
	public static final String SNOWOWL_V1_CONTENT_TYPE = "application/vnd.com.b2international.snowowl-v1+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final String SNOMED_TERMINOLOGY_URL = "snomed-ct";
	public static final String MAIN_BRANCH_URL = SNOMED_TERMINOLOGY_URL + "/MAIN";
	public static final String TASKS_URL = MAIN_BRANCH_URL + "/tasks";
	public static final String IMPORTS_URL = SNOMED_TERMINOLOGY_URL + "/imports";
	public static final String CLASSIFICATIONS_URL = "/classifications";
	public static final String EQUIVALENT_CONCEPTS_URL = "/equivalent-concepts";
	public static final String RELATIONSHIP_CHANGES_URL = "/relationship-changes";

	private final String snowOwlUrl;
	private final Resty resty;
	private String reasonerId;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnowOwlRestClient(String snowOwlUrl, String username, String password) {
		this.snowOwlUrl = snowOwlUrl;
		this.resty = new RestyMod(new Resty.Option() {
			@Override
			public void apply(URLConnection aConnection) {
				aConnection.addRequestProperty("Accept", SNOWOWL_V1_CONTENT_TYPE);
			}
		});
		resty.authenticate(snowOwlUrl, username, password.toCharArray());
	}

	@SuppressWarnings("unchecked")
	public List<String> listBranches() throws SnowOwlRestClientException {
		try {
			JSONResource tasks = resty.json(snowOwlUrl + TASKS_URL);
			return (List<String>) tasks.get("items.taskId");
		} catch (IOException e) {
			throw new SnowOwlRestClientException("Failed to retrieve branch list.", e);
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Failed to parse branch list.", e);
		}
	}

	/**
	 * @param branchName
	 * @return true if branch created, false if already existed
	 * @throws SnowOwlRestClientException
	 */
	public boolean getCreateBranch(String branchName) throws SnowOwlRestClientException {
		// http://localhost:8080/snowowl/snomed-ct/MAIN/tasks/create-1
		try {
			resty.json(snowOwlUrl + TASKS_URL + "/" + branchName);
			logger.info("Branch exists {}", branchName);
			return false;
		} catch (IOException e) {
			if (e.getCause() instanceof FileNotFoundException) {
				// Branch not found. Create.
				try {
					String json = "{\n" +
							"  \"description\": \"" + branchName + "\",\n" +
							"  \"taskId\": \"" + branchName + "\"\n" +
							"}";
					resty.json(snowOwlUrl + TASKS_URL, RestyHelper.content(new JSONObject(json), SNOWOWL_V1_CONTENT_TYPE));
					logger.info("Created branch {}", branchName);
					return true;
				} catch (IOException | JSONException e1) {
					throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e1);
				}
			} else {
				throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e);
			}
		}
	}

	public boolean importRF2Archive(String branchName, final InputStream rf2ZipFileStream) throws SnowOwlRestClientException {
		Assert.notNull(rf2ZipFileStream, "Archive to import should not be null.");

		try {
			// Create import
			logger.info("Create import, branch name '{}'", branchName);
			String jsonString = "{\n" +
					"  \"version\": \"MAIN\",\n" +
					"  \"type\": \"DELTA\",\n" +
					"  \"taskId\": \"" + branchName + "\",\n" +
					"  \"languageRefSetId\": \"900000000000509007\",\n" +
					"  \"createVersions\": false\n" +
					"}\n";

			resty.withHeader("Accept", SNOWOWL_V1_CONTENT_TYPE);
			JSONResource json = resty.json(snowOwlUrl + IMPORTS_URL, RestyHelper.content(new JSONObject(jsonString), SNOWOWL_V1_CONTENT_TYPE));
			String location = json.getUrlConnection().getHeaderField("Location");
			String importId = location.substring(location.lastIndexOf("/") + 1);

			// Create file from stream
			File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
			File tempFile = new File(tempDirectory, "SnomedCT_Release_INT_20150101.zip");
			try {
				try (FileOutputStream output = new FileOutputStream(tempFile)) {
					IOUtils.copy(rf2ZipFileStream, output);
				}

				// Post file to TS
				MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
				multipartEntityBuilder.addBinaryBody("file", tempFile, ContentType.create("application/zip"), tempFile.getName());
				multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				HttpEntity httpEntity = multipartEntityBuilder.build();
				resty.withHeader("Accept", ANY_CONTENT_TYPE);
				resty.json(snowOwlUrl + IMPORTS_URL + "/" + importId + "/archive", new HttpEntityContent(httpEntity));

			} finally {
				tempFile.delete();
				tempDirectory.delete();
			}

			// Poll import entity until complete or times-out
			logger.info("SnowOwl processing import, this will probably take a few minutes. (Import ID '{}')", importId);
			return waitForCompleteStatus(snowOwlUrl + IMPORTS_URL + "/" + importId, getTimeoutDate(IMPORT_TIMEOUT_MINUTES), "import");
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Import failed.", e);
		}
	}

	public ClassificationResults classify(String branchName) throws SnowOwlRestClientException, InterruptedException {
		ClassificationResults results = new ClassificationResults();
		String classificationLocation;
		try {
			String requestJson = "{ reasonerId: \"" + reasonerId + "\" }";
			JSONResource jsonResponse = resty.json(snowOwlUrl + TASKS_URL + "/" + branchName + CLASSIFICATIONS_URL, RestyHelper.content(new JSONObject(requestJson), SNOWOWL_V1_CONTENT_TYPE));
			classificationLocation = jsonResponse.getUrlConnection().getHeaderField("Location");
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Create classification failed.", e);
		}

		logger.info("SnowOwl classifier running, this will probably take a few minutes. (Classification URL '{}')", classificationLocation);
		boolean classifierCompleted = waitForCompleteStatus(classificationLocation, getTimeoutDate(CLASSIFICATION_TIMEOUT_MINUTES), "classifier");
		if (classifierCompleted) {
			try {
				// Check equivalent concepts
				results.setEquivalentConceptsFound(!checkNoItems(classificationLocation + EQUIVALENT_CONCEPTS_URL));
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve equivalent concepts of classification.", e);
			}
			try {
				// Check relationship changes
				Integer total = (Integer) resty.json(classificationLocation + RELATIONSHIP_CHANGES_URL).get("total");
				results.setRelationshipChangesFound(total != 0);
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve relationship changes of classification.", e);
			}
			return results;
		} else {
			throw new SnowOwlRestClientException("Classification failed, see SnowOwl logs for details.");
		}
	}

	private boolean checkNoItems(String url) throws Exception {
		JSONResource jsonResource = resty.json(url);
		try {
			JSONArray items = (JSONArray) jsonResource.get("items");
			return items == null || items.length() == 0;
		} catch (JSONException e) {
			// this gets thrown when the attribute does not exist
			return true;
		}
	}

	private boolean waitForCompleteStatus(String url, Date timeoutDate, final String waitingFor) throws SnowOwlRestClientException, InterruptedException {
		String status = "";
		boolean complete = false;
		while (!complete) {
			try {
				status = (String) resty.json(url).get("status");
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Rest client error while checking status of " + waitingFor + ".", e);
			}
			complete = !"RUNNING".equals(status);
			if (new Date().after(timeoutDate)) {
				throw new SnowOwlRestClientException("Client timeout waiting for " + waitingFor + ".");
			}
			Thread.sleep(1000 * 10);
		}
		return "COMPLETED".equals(status);
	}

	private Date getTimeoutDate(int importTimeoutMinutes) {
		GregorianCalendar timeoutCalendar = new GregorianCalendar();
		timeoutCalendar.add(Calendar.MINUTE, importTimeoutMinutes);
		return timeoutCalendar.getTime();
	}

	public void setReasonerId(String reasonerId) {
		this.reasonerId = reasonerId;
	}

	public String getReasonerId() {
		return reasonerId;
	}
}
