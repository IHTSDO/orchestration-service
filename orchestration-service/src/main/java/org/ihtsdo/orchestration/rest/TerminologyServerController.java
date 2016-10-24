package org.ihtsdo.orchestration.rest;

import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.ASSERTION_GROUP_NAMES;
import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.DEPENDENCY_RELEASE;
import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.PREVIOUS_RELEASE;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.orchestration.rest.util.PathUtil;
import org.ihtsdo.orchestration.service.ReleaseService;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import us.monoid.json.JSONException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController
@RequestMapping("/REST/termserver")
public class TerminologyServerController {

	private static final String INTERNATIONAL = "international";

	@Autowired
	private ValidationService validationService;

	@Autowired
	private ReleaseService releaseService;
	
	@Autowired
	private String failureExportMax;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BRANCH_PATH_KEY = "branchPath";
	public static final String EFFECTIVE_DATE_KEY = "effective-date";
	public static final String PRODUCT_NAME = "productName";
	public static final String EXPORT_TYPE = "exportType"; // PUBLISHED or UNPUBLISHED
	public static final String SHORT_NAME ="shortname";

	private static final String RELEASE_CENTER = "releaseCenter";

	@RequestMapping(value = "/validations", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void createValidation(@RequestBody(required = false) String json) throws BadRequestException, EntityAlreadyExistsException {
		logger.info("Create validation '{}'", json);
		if (json != null) {
			JsonElement options = new JsonParser().parse(json);
			JsonObject jsonObj = options.getAsJsonObject();
			String branchPath = getRequiredParamString(jsonObj, BRANCH_PATH_KEY);
			String effectiveDate = getOptionalParamString(jsonObj, EFFECTIVE_DATE_KEY);
			ValidationConfiguration validationConfig = new ValidationConfiguration();
			validationConfig.setFailureExportMax(failureExportMax);
			String previousRelease = getRequiredParamString(jsonObj, PREVIOUS_RELEASE);
			String dependencyRelease = getOptionalParamString(jsonObj, DEPENDENCY_RELEASE);
			if (dependencyRelease != null) {
				validationConfig.setExtensionDependencyRelease(dependencyRelease);
				validationConfig.setPreviousExtensionRelease(previousRelease);
			} else {
				validationConfig.setPreviousInternationalRelease(previousRelease);
			}
			
			String releaseCenter = getOptionalParamString(jsonObj, SHORT_NAME);
			if (releaseCenter != null) {
				validationConfig.setReleaseCenter(releaseCenter);
			} else {
				validationConfig.setReleaseCenter(INTERNATIONAL);
			}
			
			String assertionGroups = getRequiredParamString(jsonObj, ASSERTION_GROUP_NAMES);
			if (assertionGroups != null && !assertionGroups.trim().isEmpty()) {
				validationConfig.setAssertionGroupNames(assertionGroups);
			}
			validationConfig.setReleaseDate(effectiveDate);
			validationService.validate(validationConfig, branchPath, effectiveDate, null);
		}
	}

	@RequestMapping(value = "/release", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void createRelease(@RequestBody(required = false) String json) throws IOException, JSONException, BusinessServiceException {
		logger.info("Create validation '{}'", json);
		if (json != null) {
			JsonElement options = new JsonParser().parse(json);
			JsonObject jsonObj = options.getAsJsonObject();
			String branchPath = getRequiredParamString(jsonObj, BRANCH_PATH_KEY);
			String effectiveDate = getRequiredParamString(jsonObj, EFFECTIVE_DATE_KEY);
			String productName = getRequiredParamString(jsonObj, PRODUCT_NAME);
			String exportTypeStr = getRequiredParamString(jsonObj, EXPORT_TYPE);
			String releaseCenter = getOptionalParamString(jsonObj, RELEASE_CENTER);
			if (releaseCenter == null) {
				//default to international
				releaseCenter = INTERNATIONAL;
			}
			SnowOwlRestClient.ExportType exportType = SnowOwlRestClient.ExportType.valueOf(exportTypeStr);
			// Passing null callback as this request has not come from a termserver user
			releaseService.release(productName, releaseCenter, branchPath, effectiveDate, exportType, null);
		}
	}

	@RequestMapping(value = "/validations/**/latest", method = RequestMethod.GET)
	public ResponseEntity<ValidationReportDTO> getLatestValidation(HttpServletRequest request) throws ResourceNotFoundException, IOException {
		String path = (String) request.getAttribute(
				HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		final String after = "/validations/";
		final String before = "/latest";
		path = PathUtil.getStringBetween(path, before, after);
		final ValidationReportDTO latestValidation = validationService.getLatestValidation(path);
		if (latestValidation != null) {
			logger.debug("Got latest validation for '{}' - {} ", path, latestValidation.getExecutionStatus() );
			return new ResponseEntity<ValidationReportDTO>(latestValidation,HttpStatus.OK);
		} else {
			logger.info("Validation for path '" + path + "' not found.");
			return new ResponseEntity<ValidationReportDTO>(HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/validations/bulk/latest/statuses", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<String> getLatestValidationStatuses(@RequestParam String[] paths) throws ResourceNotFoundException, IOException {
		logger.debug("Getting latest validation statuses for paths '{}'", (Object[]) paths);
		return validationService.getLatestValidationStatuses(Arrays.asList(paths));
	}

	private String getRequiredParamString(JsonObject jsonObj, String key) throws BadRequestException {
		if (jsonObj.has(key)) {
			return jsonObj.getAsJsonPrimitive(key).getAsString();
		} else {
			throw new BadRequestException(key + " param is required");
		}
	}
	

	private String getOptionalParamString(JsonObject jsonObj,
			String key) {
		if (jsonObj.has(key)) {
			return jsonObj.getAsJsonPrimitive(key).getAsString();
		} else {
			return null;
		}
	}

}
