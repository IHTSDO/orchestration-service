package org.ihtsdo.orchestration.importer;

import org.ihtsdo.orchestration.clients.snowowl.SnowOwlRestClient;
import org.ihtsdo.ts.importfilter.ImportFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;

public class DevTestTool {

	@Autowired
	private ImportFilterService importFilterService;

	@Autowired
	private SnowOwlRestClient tsClient;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public void run() {
		try {
			InputStream selectionArchiveStream = importFilterService.getSelectionArchive("-20150312_133849_322");
			boolean importSuccessful = tsClient.importRF2Archive("test", "test", selectionArchiveStream);
		} catch (Exception e) {
			logger.error("Bang", e);
		}
	}

}
