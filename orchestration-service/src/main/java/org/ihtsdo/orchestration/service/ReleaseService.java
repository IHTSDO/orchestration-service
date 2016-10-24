package org.ihtsdo.orchestration.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.dao.OrchestrationProcessReportDAO;
import org.ihtsdo.otf.rest.client.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import us.monoid.json.JSONException;

/**
 * Release is very similar to Validation, but assumes that configuration has been performed externally eg via script
 */
public class ReleaseService {

	public static final String RELEASE_PROCESS = "release";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected OrchestrationProcessReportDAO processReportDAO;

	@Autowired
	protected SnowOwlRestClient snowOwlRestClient;

	@Autowired
	protected SRSRestClient srsClient;

	@Autowired
	protected RVFRestClient rvfClient;
	
	@Autowired
	private String failureExportMax;


	public synchronized void release(String productName, String releaseCenter, String branchPath, String effectiveDate, SnowOwlRestClient.ExportType exportType,
			OrchestrationCallback callback)
			throws IOException, JSONException, BusinessServiceException {
		Assert.notNull(branchPath);
		// Check we either don't have a current status, or the status is FAILED or COMPLETE
		String status = processReportDAO.getStatus(branchPath, RELEASE_PROCESS);
		if (status != null && !OrchProcStatus.isFinalState(status)) {
			throw new EntityAlreadyExistsException("An in-progress release has been detected for " + branchPath + " at state " + status);
		}

		// Check to make sure this product exists in SRS because we won't configure a new one!
		srsClient.checkProductExists(productName, releaseCenter,false);

		// Update S3 location
		processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ReleaseRunner(productName, releaseCenter, branchPath, effectiveDate, exportType, callback))).start();
	}


	private class ReleaseRunner implements Runnable {

		private final String branchPath;
		private final String effectiveDate;
		private final String productName;
		private final SnowOwlRestClient.ExportType exportType;
		private final OrchestrationCallback callback;
		private String releaseCenter;

		private ReleaseRunner(String productName, String releaseCenter, String branchPath, String effectiveDate, SnowOwlRestClient.ExportType exportType,
				OrchestrationCallback callback) {
			this.branchPath = branchPath;
			this.effectiveDate = effectiveDate;
			this.callback = callback;
			this.productName = productName;
			this.exportType = exportType;
			this.releaseCenter = releaseCenter;
		}

		@Override
		public void run() {

			OrchProcStatus finalOrchProcStatus = OrchProcStatus.FAILED;
			try {
				// Export
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.EXPORTING.toString(), null);
				File exportArchive = snowOwlRestClient.export(branchPath, effectiveDate, exportType, SnowOwlRestClient.ExtractType.DELTA);

				// Create files for SRS / Initiate SRS
				SRSProjectConfiguration config = new SRSProjectConfiguration(productName, this.releaseCenter, this.effectiveDate);
				config.setFailureExportMax(failureExportMax);
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.BUILD_INITIATING.toString(), null);
				boolean includeExternallyMaintainedFiles = true;
				srsClient.prepareSRSFiles(exportArchive, config, includeExternallyMaintainedFiles);

				// Note that unlike validation, we will not configure the build here.
				// That will be done externally eg srs-script-client calls.

				// Trigger SRS
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.BUILDING.toString(), null);
				Map<String, String> srsResponse = srsClient.runBuild(config);

				// Wait for RVF response
				// Did we obtain the RVF location for the next step in the process to poll?
				if (srsResponse.containsKey(SRSRestClient.RVF_RESPONSE)) {
					processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.VALIDATING.toString(), null);
					JSONObject rvfReport = rvfClient.waitForResponse(srsResponse.get(SRSRestClient.RVF_RESPONSE));
					processReportDAO.saveReport(branchPath, RELEASE_PROCESS, rvfReport);
					processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.COMPLETED.toString(), null);
					finalOrchProcStatus = OrchProcStatus.COMPLETED;
				} else {
					String error = "Did not find RVF Response location in SRS Client Response";
					processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.FAILED.toString(), error);
				}
			} catch (Exception e) {
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.FAILED.toString(), e.getMessage());
				logger.error("Release of {} failed.", branchPath, e);
			}

			if (callback != null) {
				callback.complete(finalOrchProcStatus);
			}
		}
	}

}
