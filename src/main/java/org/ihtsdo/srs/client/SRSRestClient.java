package org.ihtsdo.srs.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.ts.importer.clients.resty.HttpEntityContent;
import org.ihtsdo.ts.importer.clients.resty.RestyHelper;
import org.ihtsdo.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.mime.MultipartContent;

public class SRSRestClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected static final String CONTENT_TYPE_ANY = "*/*";
	protected static final String CONTENT_TYPE_XML = "text/xml";
	protected static final String CONTENT_TYPE_JSON = "application/json";
	protected static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
	protected static final String CONTENT_TYPE_TEXT = "text/plain;charset=UTF-8";

	private static final String BLANK_MANIFEST = "/manifest_no_date.xml";
	private static final String DATE_MARKER = "########";
	private static final String MANIFEST_ENDPOINT = "manifest";
	private static final String INPUT_FILES_ENDPOINT = "inputfiles";
	private static final String DELETE_FILTER = "/*.txt";
	private static final String BUILD_ENDPOINT = "builds";
	private static final String AUTHENTICATE_ENDPOINT = "login";
	private static final String TRIGGER_BUILD_ENDPOINT = "/trigger";
	private static final String PRODUCT_CONFIGURATION_ENDPOINT = "/configuration";
	private static final String AUTHENTICATION_TOKEN = "authenticationToken";
	private static final String ID = "id";
	private static final char[] BLANK_PASSWORD = "".toCharArray();
	private static final byte[] EMPTY_CONTENT_ARRAY = new byte[0];
	private static final Content EMPTY_CONTENT = new Content(CONTENT_TYPE_TEXT, EMPTY_CONTENT_ARRAY);

	private final String srsRootURL;
	private final String srsProductURL;
	private final String username;
	private final String password;

	private final RestyHelper resty;

	public static final String RVF_RESPONSE = "buildReport.rvf_response";
	protected static final String[] ITEMS_OF_INTEREST = { "outputfiles_url", "status", "logs_url", RVF_RESPONSE, "buildReport.Message" };

	public SRSRestClient(String srsRootURL, String srsProduct, String username, String password) {
		this.srsRootURL = srsRootURL;
		this.srsProductURL = srsRootURL + srsProduct;
		this.username = username;
		this.password = password;
		this.resty = new RestyHelper();
	}

	/**
	 * Constructor to use for local isolated testing.
	 */
	public SRSRestClient() {
		srsRootURL = null;
		srsProductURL = null;
		username = null;
		password = null;
		resty = null;
	}

	public Map<String, String> runDailyBuild(File srsFilesDir, String releaseDate) throws Exception {

		logger.info("Running daily build for {} with files uploaded from: {}", releaseDate, srsFilesDir.getAbsolutePath());
		// Authentication first
		MultipartContent credentials = Resty.form(Resty.data("username", username), Resty.data("password", password));
		JSONResource json = resty.json(srsRootURL + AUTHENTICATE_ENDPOINT, credentials);
		Object authToken = json.get(AUTHENTICATION_TOKEN);
		Assert.notNull(authToken, "Failed to recover SRS Authentication from " + srsRootURL);

		// Now the token received can be set as the username for all subsequent interactions. Blank password.
		resty.authenticate(srsRootURL, authToken.toString(), BLANK_PASSWORD);

		// Lets upload the manifest first
		File configuredManifest = configureManifest(releaseDate);
		uploadFile(srsProductURL + MANIFEST_ENDPOINT, configuredManifest);
		configuredManifest.delete();

		boolean ensureSuccess = true;

		// Need to tell the product what release date it's targeting
		String releaseDateISO = DateUtils.formatAsISO(releaseDate);
		logger.debug("Setting product effective time to {}", releaseDateISO);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("effectiveTime", releaseDateISO);
		resty.json(srsProductURL + PRODUCT_CONFIGURATION_ENDPOINT, jsonObj, CONTENT_TYPE_JSON);

		// Delete any previously uploaded input files
		logger.debug("Deleting previous input files");
		resty.json(srsProductURL + INPUT_FILES_ENDPOINT + DELETE_FILTER, Resty.delete());

		// Now everything in the target directory
		uploadFiles(srsFilesDir, srsProductURL + INPUT_FILES_ENDPOINT);
		// And we can delete that too, now that a copy is safely stored in S3
		FileUtils.deleteDirectory(srsFilesDir);

		// Create a build. Pass blank content to encourage Resty to use POST
		json = resty.json(srsProductURL + BUILD_ENDPOINT, EMPTY_CONTENT);
		Object buildId = json.get(ID);
		Assert.notNull(buildId, "Failed to recover create build at: " + srsProductURL);

		// Trigger Build
		String buildTriggerURL = srsProductURL + BUILD_ENDPOINT + "/" + buildId + TRIGGER_BUILD_ENDPOINT;
		logger.debug("Triggering Build: {}", buildTriggerURL);
		json = resty.json(buildTriggerURL, EMPTY_CONTENT);
		logger.debug("Build trigger returned: {}", json.object().toString(2));

		return recoverItemsOfInterest(json);
	}

	protected Map<String, String> recoverItemsOfInterest(JSONResource json) throws JSONException, IOException {
		// Recover some things the users might be interested in, to store in the Jira Ticket
		Map<String, String> response = new HashMap<String, String>();
		int itemsFound = 0;
		for (String item : ITEMS_OF_INTEREST) {
			try {
				Object value = json.get(item);
				response.put(item, value.toString());
				itemsFound++;
			} catch (Exception e) {
				logger.error("Failed to recover item of interest from SRS Trigger Response: {} ", item, e);
			}
		}

		if (itemsFound < ITEMS_OF_INTEREST.length) {
			logger.warn("Items of interest issues encountered with JSON: {}", json.object().toString(2));
		}

		if (itemsFound == 0) {
			response.put("Error", "Failed to recover any items of interest from the SRS Response");
		}
		return response;
	}

	private void uploadFiles(File srsFilesDir, String url) throws ProcessingException {
		Assert.isTrue(srsFilesDir.isDirectory(), srsFilesDir.getAbsolutePath() + " must be a directory in order to use it to upload files.");
		for (File thisFile : srsFilesDir.listFiles()) {
			if (thisFile.exists() && !thisFile.isDirectory()) {
				uploadFile(url, thisFile);
			}
		}
	}

	private void uploadFile(String url, File file) throws ProcessingException {

		Assert.isTrue(file.exists(), "File for upload to " + url + " was found to not exist at location: " + file.getAbsolutePath());
		Assert.isTrue(!file.isDirectory(), "File for upload to " + url + " was found to be a directory: " + file.getAbsolutePath());
		boolean ensureSuccess = true;
		try {
			logger.debug("Uploading file to {} : {} ", url, file.getAbsolutePath());
			MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
			multipartEntityBuilder.addBinaryBody("file", file, ContentType.create(CONTENT_TYPE_MULTIPART), file.getName());
			// multipartEntityBuilder.addPart("file", new FileBody(file));
			multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			HttpEntity httpEntity = multipartEntityBuilder.build();
			resty.withHeader("Accept", CONTENT_TYPE_ANY);
			resty.json(url, new HttpEntityContent(httpEntity));
		} catch (Exception e) {
			throw new ProcessingException("Failed to upload " + file.getAbsolutePath(), e);
		}
	}

	protected File configureManifest(String releaseDate) throws IOException {
		// We need to build a manifest file containing the target release date
		InputStream is = getClass().getResourceAsStream(BLANK_MANIFEST);
		Assert.notNull(is, "Failed to load blank manifest.");
		String content = IOUtils.toString(is, StandardCharsets.UTF_8);
		content = content.replaceAll(DATE_MARKER, releaseDate);
		File configuredManifest = File.createTempFile("manifest_" + releaseDate, ".xml");
		Files.write(configuredManifest.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return configuredManifest;
	}

}
