package org.ihtsdo.orchestration.schedule;

import org.ihtsdo.orchestration.importer.ImporterService;
import org.ihtsdo.orchestration.clients.jira.JiraProjectSync;
import org.ihtsdo.orchestration.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class StartDailyExport implements Runnable {

	@Autowired
	private ImporterService importerService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private String jiraDailyExportProjectKey;

	@Autowired
	private JiraProjectSync jira;

	@Override
	public void run() {
		logger.info("Scheduled export triggered - creating Jira Ticket");
		try {
			String taskLabel = "Daily Export - " + DateUtils.today(DateUtils.YYYYMMDD);
			jira.createTask(jiraDailyExportProjectKey, taskLabel);
		} catch (Exception e) {
			logger.error("Failed to initiate daily export", e);
		}
	}

}
