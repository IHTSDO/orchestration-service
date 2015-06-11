package org.ihtsdo.orchestration.clients.snowowl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.ihtsdo.orchestration.clients.common.resty.HttpEntityContent;
import org.ihtsdo.orchestration.clients.common.resty.RestyHelper;
import org.ihtsdo.orchestration.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.JSONResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SnowOwlRestClient {

	public static final String SNOWOWL_CONTENT_TYPE = "application/vnd.com.b2international.snowowl+json";
	public static final String ANY_CONTENT_TYPE = "*/*";
	public static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");
	public static final String US_EN_LANG_REFSET = "900000000000509007";

	public enum ExtractType {
		DELTA, SNAPSHOT, FULL;
	};

	public enum ProcessingStatus {
		COMPLETED, SAVED
	}

	private final SnowOwlRestUrlHelper urlHelper;
	private final RestyHelper resty;
	private String reasonerId;
	private String logPath;
	private String rolloverLogPath;
	private final Gson gson;
	private int importTimeoutMinutes;
	private int classificationTimeoutMinutes;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnowOwlRestClient(String snowOwlUrl, String username, String password) {
		snowOwlUrl = SnowOwlRestUrlHelper.removeTrailingSlash(snowOwlUrl);
		urlHelper = new SnowOwlRestUrlHelper(snowOwlUrl);
		this.resty = new RestyHelper(ANY_CONTENT_TYPE);
		resty.authenticate(snowOwlUrl, username, password.toCharArray());
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	public void createProjectBranch(String branchName) throws SnowOwlRestClientException {
		createBranch(urlHelper.getMainBranchPath(), branchName);
	}

	public void createProjectBranchIfNeeded(String projectName) throws SnowOwlRestClientException {
		if (!listProjectBranches().contains(projectName)) {
			createProjectBranch(projectName);
		}
	}

	private void createBranch(String parentBranch, String newBranchName) throws SnowOwlRestClientException {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("parent", parentBranch);
			jsonObject.put("name", newBranchName);
			resty.json(urlHelper.getBranchesUrl(), RestyHelper.content((jsonObject), SNOWOWL_CONTENT_TYPE));
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Failed to create branch " + newBranchName + ", parent branch " + parentBranch, e);
		}
	}

	public List<String> listProjectBranches() throws SnowOwlRestClientException {
		return listBranchDirectChildren(urlHelper.getMainBranchPath());
	}

	public List<String> listProjectTasks(String projectName) throws SnowOwlRestClientException {
		return listBranchDirectChildren(urlHelper.getBranchPath(projectName));
	}

	private List<String> listBranchDirectChildren(String branchPath) throws SnowOwlRestClientException {
		try {
			List<String> projectNames = new ArrayList<>();
			JSONResource json = resty.json(urlHelper.getBranchChildrenUrl(branchPath));
			try {
				@SuppressWarnings("unchecked")
				List<String> childBranchPaths = (List<String>) json.get("items.path");
				for (String childBranchPath : childBranchPaths) {
					String branchName = childBranchPath.substring((branchPath + "/").length());
					if (!branchName.contains("/")) {
						projectNames.add(branchName);
					}
				}
			} catch (JSONException e) {
				// this thrown if there are no items.. do nothing
			}
			return projectNames;
		} catch (IOException e) {
			throw new SnowOwlRestClientException("Failed to retrieve branch list.", e);
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Failed to parse branch list.", e);
		}
	}

	public void deleteProjectBranch(String projectBranchName) throws SnowOwlRestClientException {
		deleteBranch(projectBranchName);
	}

	public void deleteTaskBranch(String projectName, String taskName) throws SnowOwlRestClientException {
		deleteBranch(projectName + "/" + taskName);
	}

	private void deleteBranch(String branchPathRelativeToMain) throws SnowOwlRestClientException {
		try {
			resty.json(urlHelper.getBranchUrlRelativeToMain(branchPathRelativeToMain), RestyHelper.delete());
		} catch (IOException e) {
			throw new SnowOwlRestClientException("Failed to delete branch " + branchPathRelativeToMain, e);
		}
	}

	public void createProjectTask(String projectName, String taskName) throws SnowOwlRestClientException {
		createBranch(urlHelper.getBranchPath(projectName), taskName);
	}

	public void createProjectTaskIfNeeded(String projectName, String taskName) throws SnowOwlRestClientException {
		if (!listProjectTasks(projectName).contains(taskName)) {
			createProjectTask(projectName, taskName);
		}
	}

	public boolean importRF2Archive(String projectName, String taskName, final InputStream rf2ZipFileStream) throws SnowOwlRestClientException {
		Assert.notNull(rf2ZipFileStream, "Archive to import should not be null.");

		try {
			// Create import
			String branchPath = urlHelper.getBranchPath(projectName, taskName);
			logger.info("Create import, branch '{}'", branchPath);

			JSONObject params = new JSONObject();
			params.put("type", "DELTA");
			params.put("branchPath", branchPath);
			params.put("languageRefSetId", US_EN_LANG_REFSET);
			params.put("createVersions", "false");
			resty.withHeader("Accept", SNOWOWL_CONTENT_TYPE);
			JSONResource json = resty.json(urlHelper.getImportsUrl(), RestyHelper.content(params, SNOWOWL_CONTENT_TYPE));
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
				resty.json(urlHelper.getImportArchiveUrl(importId), new HttpEntityContent(httpEntity));

			} finally {
				tempFile.delete();
				tempDirectory.delete();
			}

			// Poll import entity until complete or times-out
			logger.info("SnowOwl processing import, this will probably take a few minutes. (Import ID '{}')", importId);
			return waitForStatus(urlHelper.getImportUrl(importId), getTimeoutDate(importTimeoutMinutes), ProcessingStatus.COMPLETED, "import");
		} catch (Exception e) {
			throw new SnowOwlRestClientException("Import failed.", e);
		}
	}

	public ClassificationResults classifyTask(String projectName, String taskName) throws SnowOwlRestClientException, InterruptedException {
		return classify(urlHelper.getBranchPath(projectName, taskName));
	}

	public ClassificationResults classifyProject(String projectName) throws SnowOwlRestClientException, InterruptedException {
		return classify(urlHelper.getBranchPath(projectName));
	}

	private ClassificationResults classify(String branchPath) throws SnowOwlRestClientException, InterruptedException {
		ClassificationResults results = new ClassificationResults();
		String date = SIMPLE_DATE_FORMAT.format(new Date());
		String classificationLocation;
		try {
			JSONObject requestJson = new JSONObject().put("reasonerId", reasonerId);
			String classifyURL = urlHelper.getClassificationsUrl(branchPath);
			logger.debug("Initiating classification via {}", classifyURL);
			JSONResource jsonResponse = resty.json(classifyURL, requestJson, SNOWOWL_CONTENT_TYPE);
			classificationLocation = jsonResponse.getUrlConnection().getHeaderField("Location");
			results.setClassificationId(classificationLocation.substring(classificationLocation.lastIndexOf("/") + 1));
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Create classification failed.", e);
		}

		logger.info("SnowOwl classifier running, this will probably take a few minutes. (Classification URL '{}')", classificationLocation);
		boolean classifierCompleted = waitForStatus(classificationLocation, getTimeoutDate(classificationTimeoutMinutes), ProcessingStatus.COMPLETED, "classifier");
		if (classifierCompleted) {
			try {
				// Check equivalent concepts
				JSONArray items = getItems(urlHelper.getEquivalentConceptsUrl(classificationLocation));
				boolean equivalentConceptsFound = !(items == null || items.length() == 0);
				results.setEquivalentConceptsFound(equivalentConceptsFound);
				if (equivalentConceptsFound) {
					results.setEquivalentConceptsJson(toPrettyJson(items.toString()));
				}
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve equivalent concepts of classification.", e);
			}
			try {
				// Check relationship changes
				JSONResource relationshipChangesUnlimited = resty.json(urlHelper.getRelationshipChangesFirstTenThousand(classificationLocation));
				Integer total = (Integer) relationshipChangesUnlimited.get("total");
				results.setRelationshipChangesCount(total);
				Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
				File file = new File(tempDirectory.toFile(), "relationship-changes-" + date + ".json");
				toPrettyJson(relationshipChangesUnlimited.object().toString(), file);
				results.setRelationshipChangesFile(file);
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Failed to retrieve relationship changes of classification.", e);
			}
			return results;
		} else {
			throw new SnowOwlRestClientException("Classification failed, see SnowOwl logs for details.");
		}
	}

	public void saveClassificationOfTask(String projectName, String taskName, String classificationId) throws SnowOwlRestClientException, InterruptedException {
		saveClassification(urlHelper.getClassificationUrl(projectName, taskName, classificationId));
	}

	public void saveClassificationOfProject(String projectName, String classificationId) throws SnowOwlRestClientException, InterruptedException {
		saveClassification(urlHelper.getClassificationUrl(projectName, null, classificationId));
	}

	private void saveClassification(String classificationUrl) throws SnowOwlRestClientException, InterruptedException {
		try {
			logger.debug("Saving classification via {}", classificationUrl);
			JSONObject jsonObj = new JSONObject().put("status", "SAVED");
			resty.put(classificationUrl, jsonObj, SNOWOWL_CONTENT_TYPE);
			//We'll wait the same time for saving as we do for the classification
			boolean savingCompleted = waitForStatus(classificationUrl, getTimeoutDate(classificationTimeoutMinutes),
					ProcessingStatus.SAVED, "classifier result saving");
			if (!savingCompleted) {
				throw new IOException("Classifier reported non-saved status when saving");
			}
		} catch (IOException | JSONException e) {
			throw new SnowOwlRestClientException("Failed to save classification via URL " + classificationUrl, e);
		}
	}

	private JSONArray getItems(String url) throws Exception {
		JSONResource jsonResource = resty.json(url);
		JSONArray items = null;
		try {
			items = (JSONArray) jsonResource.get("items");
		} catch (JSONException e) {
			// this gets thrown when the attribute does not exist
		}
		return items;
	}

	public File exportTask(String projectName, String taskName, ExtractType extractType) throws Exception {
		return export(projectName, taskName, extractType);
	}

	public File exportProject(String projectName, ExtractType extractType) throws Exception {
		return export(projectName, null, extractType);
	}
	
	private File export(String projectName, String branchName, ExtractType extractType) throws Exception {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("type", extractType);

		String branchPath = urlHelper.getBranchPath(projectName, branchName);
		jsonObj.put("branchPath", branchPath);

		jsonObj.put("transientEffectiveTime", DateUtils.today(DateUtils.YYYYMMDD));

		logger.info("Initiating export with json: {}", jsonObj.toString());
		JSONResource jsonResponse = resty.json(urlHelper.getExportsUrl(), RestyHelper.content(jsonObj, SNOWOWL_CONTENT_TYPE));
		Object exportLocationURLObj = jsonResponse.getUrlConnection().getHeaderField("Location");
		String exportLocationURL = exportLocationURLObj.toString() + "/archive";

		logger.debug("Recovering exported archive from {}", exportLocationURL);
		resty.withHeader("Accept", ANY_CONTENT_TYPE);
		BinaryResource archiveResource = resty.bytes(exportLocationURL);
		File archive = File.createTempFile("ts-extract", ".zip");
		archiveResource.save(archive);
		logger.debug("Extract saved to {}", archive.getAbsolutePath());
		return archive;
	}

	public void rebaseTask(String projectName, String taskName) throws IOException, JSONException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Rebasing branch {} from parent {}", taskPath, projectPath);
		merge(projectPath, taskPath);
	}

	public void mergeTaskToProject(String projectName, String taskName) throws IOException, JSONException {
		String taskPath = urlHelper.getBranchPath(projectName, taskName);
		String projectPath = urlHelper.getBranchPath(projectName);
		logger.info("Promoting branch {} to {}", taskPath, projectPath);
		merge(taskPath, projectPath);
	}

	private void merge(String sourcePath, String targetPath) throws JSONException, IOException {
		JSONObject params = new JSONObject();
		params.put("source", sourcePath);
		params.put("target", targetPath);
		resty.put(urlHelper.getMergesUrl(), params, SNOWOWL_CONTENT_TYPE);
	}

	/**
	 * Warning - this only works when the SnowOwl log is on the same machine.
	 */
	public InputStream getLogStream() throws FileNotFoundException {
		return new FileInputStream(logPath);
	}

	/**
	 * Returns stream from rollover log or null.
	 */
	public InputStream getRolloverLogStream() throws FileNotFoundException {
		if (new File(rolloverLogPath).isFile()) {
			return new FileInputStream(rolloverLogPath);
		} else {
			return null;
		}
	}

	private boolean waitForStatus(String url, Date timeoutDate, ProcessingStatus targetStatus, final String waitingFor)
			throws SnowOwlRestClientException, InterruptedException {
		String status = "";
		boolean finalStateAchieved = false;
		while (!finalStateAchieved) {
			try {
				status = (String) resty.json(url).get("status");
			} catch (Exception e) {
				throw new SnowOwlRestClientException("Rest client error while checking status of " + waitingFor + ".", e);
			}
			finalStateAchieved = !("RUNNING".equals(status) || "SCHEDULED".equals(status) || "SAVING_IN_PROGRESS".equals(status));
			if (new Date().after(timeoutDate)) {
				throw new SnowOwlRestClientException("Client timeout waiting for " + waitingFor + ".");
			}
			Thread.sleep(1000 * 10);
		}

		boolean targetStatusAchieved = targetStatus.toString().equals(status);
		if (!targetStatusAchieved) {
			logger.warn("TS reported non-complete status {} from URL {}", status, url);
		}
		return targetStatusAchieved;
	}

	private Date getTimeoutDate(int importTimeoutMinutes) {
		GregorianCalendar timeoutCalendar = new GregorianCalendar();
		timeoutCalendar.add(Calendar.MINUTE, importTimeoutMinutes);
		return timeoutCalendar.getTime();
	}

	private String toPrettyJson(String jsonString) {
		JsonElement el = new JsonParser().parse(jsonString);
		return gson.toJson(el);
	}

	private void toPrettyJson(String jsonString, File outFile) throws IOException {
		JsonElement el = new JsonParser().parse(jsonString);
		try (JsonWriter writer = new JsonWriter(new FileWriter(outFile))) {
			gson.toJson(el, writer);
		}
	}

	public void setReasonerId(String reasonerId) {
		this.reasonerId = reasonerId;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public void setRolloverLogPath(String rolloverLogPath) {
		this.rolloverLogPath = rolloverLogPath;
	}

	@Required
	public void setImportTimeoutMinutes(int importTimeoutMinutes) {
		this.importTimeoutMinutes = importTimeoutMinutes;
	}

	@Required
	public void setClassificationTimeoutMinutes(int classificationTimeoutMinutes) {
		this.classificationTimeoutMinutes = classificationTimeoutMinutes;
	}
}
