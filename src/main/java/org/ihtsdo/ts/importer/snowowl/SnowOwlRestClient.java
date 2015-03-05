package org.ihtsdo.ts.importer.snowowl;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.ihtsdo.ts.importer.rest.MultipartEntityContent;
import org.ihtsdo.ts.importer.rest.RestyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static us.monoid.web.Resty.content;

public class SnowOwlRestClient {

	public static final int IMPORT_TIMEOUT_MINUTES = 30;
	public static final String SNOWOWL_V1_CONTENT_TYPE = "application/vnd.com.b2international.snowowl-v1+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final String SNOMED_TERMINOLOGY_URL = "snomed-ct";
	public static final String MAIN_BRANCH_URL = SNOMED_TERMINOLOGY_URL + "/MAIN";
	public static final String TASKS_URL = MAIN_BRANCH_URL + "/tasks";
	public static final String IMPORTS_URL = SNOMED_TERMINOLOGY_URL + "/imports";

	private final String snowOwlUrl;
	private final Resty resty;

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

	public void getCreateBranch(String branchName) throws SnowOwlRestClientException {
		// http://localhost:8080/snowowl/snomed-ct/MAIN/tasks/create-1
		try {
			resty.json(snowOwlUrl + TASKS_URL + "/" + branchName);
			logger.info("Branch exists {}", branchName);
		} catch (IOException e) {
			if (e.getCause() instanceof FileNotFoundException) {
				// Branch not found. Create.
				try {
					String json = "{\n" +
							"  \"description\": \"" + branchName + "\",\n" +
							"  \"taskId\": \"" + branchName + "\"\n" +
							"}";
					resty.json(snowOwlUrl + TASKS_URL, content(json));
					logger.info("Created branch {}", branchName);
				} catch (IOException e1) {
					throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e1);
				}
			} else {
				throw new SnowOwlRestClientException("Failed to create branch '" + branchName + "'.", e);
			}
		}
	}

	public void importRF2(String branchName, final InputStream rf2FileStream) throws SnowOwlRestClientException {
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
			logger.info("Import ID {}", importId);

			// Create file from stream
			File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
			File tempFile = new File(tempDirectory, "SnomedCT_Release_INT_20150101.zip");
			try {
				try (FileOutputStream output = new FileOutputStream(tempFile)) {
					IOUtils.copy(rf2FileStream, output);
				}

				// Post file to TS
				final MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				multipartEntity.addPart("file", new FileBody(tempFile, tempFile.getName()));
				resty.withHeader("Accept", ANY_CONTENT_TYPE);
				resty.json(snowOwlUrl + IMPORTS_URL + "/" + importId + "/archive", new MultipartEntityContent(multipartEntity));

			} finally {
				tempFile.delete();
				tempDirectory.delete();
			}

			// Poll import entity until complete or times-out
			Date timeoutDate = getTimeoutDate(IMPORT_TIMEOUT_MINUTES);
			boolean complete = false;
			while (!complete) {
				complete = !"RUNNING".equals(resty.json(snowOwlUrl + IMPORTS_URL + "/" + importId).get("status"));
				if (new Date().after(timeoutDate)) {
					throw new SnowOwlRestClientException("Client maximum import time reached.");
				}
				Thread.sleep(1000 * 10);
			}

		} catch (Exception e) {
			throw new SnowOwlRestClientException("Import failed.", e);
		}
	}

	private Date getTimeoutDate(int importTimeoutMinutes) {
		GregorianCalendar timeoutCalendar = new GregorianCalendar();
		timeoutCalendar.add(Calendar.MINUTE, importTimeoutMinutes);
		return timeoutCalendar.getTime();
	}

}
