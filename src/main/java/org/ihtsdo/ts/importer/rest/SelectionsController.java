package org.ihtsdo.ts.importer.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ihtsdo.ts.importer.ImporterService;
import org.ihtsdo.ts.importfilter.LoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/REST/selections")
public class SelectionsController {

	@Autowired
	private ImporterService importerService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@RequestMapping(value = "create", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	String triggerImport(@RequestBody(required = false) String json) throws IOException, LoadException {

		Set<Long> selectConceptIdsOverride = null;
		if (json != null) {
			JsonElement options = new JsonParser().parse(json);
			JsonObject asJsonObject = options.getAsJsonObject();
			JsonArray selectConceptIds = asJsonObject.getAsJsonArray("selectConceptIds");
			if (selectConceptIds != null) {
				selectConceptIdsOverride = new HashSet<>();
				for (JsonElement selectConceptId : selectConceptIds) {
					long asLong = selectConceptId.getAsLong();
					selectConceptIdsOverride.add(asLong);
				}
			}
		}

		logger.info("selectConceptIdsOverride = {}", selectConceptIdsOverride);
		importerService.importCompletedWBContentAsync(selectConceptIdsOverride);
		return "Selection creation process started";
	}

}
