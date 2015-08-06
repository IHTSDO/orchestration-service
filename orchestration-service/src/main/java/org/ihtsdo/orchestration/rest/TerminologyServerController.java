package org.ihtsdo.orchestration.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ihtsdo.orchestration.model.ValidationReportDTO;
import org.ihtsdo.orchestration.rest.util.PathUtil;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/REST/termserver")
public class TerminologyServerController {

	@Autowired
	private ValidationService validationService;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BRANCH_PATH_KEY = "branchPath";

	@RequestMapping(value = "/validations", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void createValidation(@RequestBody(required = false) String json) throws BadRequestException, EntityAlreadyExistsException {
		logger.info("Create validation '{}'", json);
		if (json != null) {
			JsonElement options = new JsonParser().parse(json);
			JsonObject jsonObj = options.getAsJsonObject();
			String branchPath = getRequiredParamString(jsonObj, BRANCH_PATH_KEY);
			validationService.validate(branchPath);
		}
	}

	@RequestMapping(value = "/validations/**/latest", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public ValidationReportDTO getLatestValidation(HttpServletRequest request) throws ResourceNotFoundException, IOException {
		String path = (String) request.getAttribute(
				HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		final String after = "/validations/";
		final String before = "/latest";
		path = PathUtil.getStringBetween(path, before, after);
		logger.info("Get latest validation for '{}'", path);
		final ValidationReportDTO latestValidation = validationService.getLatestValidation(path);
		if (latestValidation != null) {
			return latestValidation;
		} else {
			throw new ResourceNotFoundException("Validation for path '" + path + "' not found.");
		}
	}

	private String getRequiredParamString(JsonObject jsonObj, String branchPathKey) throws BadRequestException {
		if (jsonObj.has(branchPathKey)) {
			return jsonObj.getAsJsonPrimitive(branchPathKey).getAsString();
		} else {
			throw new BadRequestException(branchPathKey + " param is required");
		}
	}


}
