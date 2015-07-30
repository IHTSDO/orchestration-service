package org.ihtsdo.orchestration.rest;

import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController
@RequestMapping("/REST/ts")
public class TerminologyServerController {

	@Autowired
	private ValidationService validationService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BRANCH_PATH_KEY = "branchPath";

	@RequestMapping(value = "/validate", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	void validate(@RequestBody(required = false) String json) throws BadRequestException, EntityAlreadyExistsException {

		if (json != null) {
			JsonElement options = new JsonParser().parse(json);
			JsonObject jsonObj = options.getAsJsonObject();
			if (jsonObj.has(BRANCH_PATH_KEY)) {
				String branchPath = jsonObj.getAsJsonPrimitive(BRANCH_PATH_KEY).getAsString();
				validationService.validate(branchPath);
				return;
			}
		}

		throw new BadRequestException("No branchPath detected in request to validate");
	}

}
