package org.ihtsdo.ts.importer.rest;

import org.ihtsdo.ts.importer.ImporterService;
import org.ihtsdo.ts.importfilter.LoadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/REST/selections")
public class SelectionsController {

	@Autowired
	private ImporterService importerService;

	@RequestMapping(value = "create", method = RequestMethod.POST)
	String triggerImport() throws IOException, LoadException {
		importerService.importCompletedWBContentAsync();
		return "Selection creation process started";
	}

}
