package org.ihtsdo.orchestration.rest;

import org.ihtsdo.orchestration.dao.TermserverReleaseRequestPojo;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.orchestration.rest.util.PathUtil;
import org.ihtsdo.orchestration.service.ReleaseService;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
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

@RestController
@RequestMapping("/REST/termserver")
public class TerminologyServerController {

	private static final String INTERNATIONAL = "international";

	@Autowired
	private ValidationService validationService;

	@Autowired
	private ReleaseService releaseService;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BRANCH_PATH_KEY = "branchPath";
	public static final String EFFECTIVE_DATE_KEY = "effective-date";
	public static final String SHORT_NAME ="shortname";

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
		if (request.getAuthToken() == null || request.getAuthToken().trim().isEmpty()) {
			throw new IllegalArgumentException("X-AUTH-token must be specified but was " + request.getAuthToken());
		}
		if (releaseCenter == null) {
			//default to international
			releaseCenter = INTERNATIONAL;
		}
		// Passing null callback as this request has not come from a termserver user
		releaseService.release(productName, releaseCenter, branchPath, effectiveDate, excludedModuleIds, exportCategory, request.getAuthToken(), null);
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
}
