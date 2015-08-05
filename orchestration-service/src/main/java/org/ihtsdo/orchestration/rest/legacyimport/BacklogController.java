package org.ihtsdo.orchestration.rest.legacyimport;

import org.ihtsdo.ts.importfilter.BacklogContentService;
import org.ihtsdo.ts.importfilter.Concept;
import org.ihtsdo.ts.importfilter.LoadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/REST/legacy-import/backlog")
public class BacklogController {

	@Autowired
	private BacklogContentService backlogContentService;

	@RequestMapping
	Set<Concept> getBreakdown() throws IOException, LoadException {
		return backlogContentService.loadConceptChanges();
	}

}
