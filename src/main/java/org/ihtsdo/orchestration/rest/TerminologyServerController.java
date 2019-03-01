package org.ihtsdo.orchestration.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.ihtsdo.orchestration.dao.TermserverReleaseRequestPojo;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.orchestration.rest.util.PathUtil;
import org.ihtsdo.orchestration.service.ReleaseService;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import us.monoid.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.*;

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
	public static final String SHORT_NAME ="shortname";

	@RequestMapping(value = "/validations", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@Deprecated
	/** Replaced by ValidationMessageHandler*/
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
				validationConfig.setDependencyRelease(dependencyRelease);
				validationConfig.setPreviousRelease(previousRelease);
			} else {
				validationConfig.setPreviousRelease(previousRelease);
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
			String rvfDroolsAssertionGroups = getRequiredParamString(jsonObj, RVF_DROOLS_ASSERTION_GROUP_NAMES);
			if (rvfDroolsAssertionGroups != null && !rvfDroolsAssertionGroups.trim().isEmpty()) {
				validationConfig.setRvfDroolsAssertionGroupNames(rvfDroolsAssertionGroups);
			}
			String defaultModuleId = getRequiredParamString(jsonObj, DEFAULT_MODULE_ID);
			if (StringUtils.isNotBlank(defaultModuleId)) {
				validationConfig.setIncludedModuleIds(defaultModuleId);
			}
			validationConfig.setReleaseDate(effectiveDate);
			validationService.validate(validationConfig, branchPath, effectiveDate, null);
		}
	}

	@RequestMapping(value = "/release", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void createRelease(@RequestBody TermserverReleaseRequestPojo request) throws IOException, JSONException, BusinessServiceException {
		logger.info("Create release '{}'", request);
		String branchPath = request.getBranchPath();
		String effectiveDate = request.getEffectiveDate();
		String productName = request.getProductName();
		SnowOwlRestClient.ExportCategory exportCategory = request.getExportCategory();
		String releaseCenter = request.getReleaseCenter();
		Set<String> excludedModuleIds = request.getExcludedModuleIds();

		if (releaseCenter == null) {
			//default to international
			releaseCenter = INTERNATIONAL;
		}
		// Passing null callback as this request has not come from a termserver user
		releaseService.release(productName, releaseCenter, branchPath, effectiveDate, excludedModuleIds, exportCategory, null);
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
			return new ResponseEntity<>(latestValidation,HttpStatus.OK);
		} else {
			logger.info("Validation for path '" + path + "' not found.");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
