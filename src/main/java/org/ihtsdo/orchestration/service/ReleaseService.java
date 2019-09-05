package org.ihtsdo.orchestration.service;

import org.ihtsdo.orchestration.clients.rvf.RVFRestClient;
import org.ihtsdo.orchestration.clients.snowowl.TerminologyServerRestClientFactory;
import org.ihtsdo.orchestration.clients.srs.SRSProjectConfiguration;
import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.ihtsdo.orchestration.dao.FileManager;
import org.ihtsdo.orchestration.dao.OrchestrationProcessReportDAO;
import org.ihtsdo.otf.constants.Concepts;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.snowowl.SnowOwlRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import us.monoid.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Release is very similar to Validation, but assumes that configuration has been performed externally eg via script
 */
public class ReleaseService {

	public static final String RELEASE_PROCESS = "release";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected OrchestrationProcessReportDAO processReportDAO;
	
	@Autowired
	private TerminologyServerRestClientFactory terminologyServerRestClientFactory;

	@Autowired
	protected SRSRestClient srsClient;

	@Autowired
	protected RVFRestClient rvfClient;
	
	@Autowired
	private String failureExportMax;
	
	private final boolean flatIndexExportStyle;

	public ReleaseService(boolean flatIndexExportStyle) {
		this.flatIndexExportStyle = flatIndexExportStyle;
	}

	public synchronized void release(String productName, String releaseCenter, String branchPath,
			String effectiveDate, Set<String> excludedModuleIds,
			SnowOwlRestClient.ExportCategory exportCategory, String authToken,
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
		
		// Create terminology server client using SSO security token
		SnowOwlRestClient snowOwlRestClient = terminologyServerRestClientFactory.getClient(authToken);

		Set<String> exportModuleIds = buildModulesList(branchPath, excludedModuleIds, snowOwlRestClient);

		// Update S3 location
		processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.SCHEDULED.toString(), null);

		// Start thread for additional processing and return immediately
		(new Thread(new ReleaseRunner(productName, releaseCenter, branchPath, effectiveDate, exportModuleIds, exportCategory, snowOwlRestClient, callback))).start();
	}

	private Set<String> buildModulesList(String branchPath, Set<String> excludedModuleIds, SnowOwlRestClient snowOwlRestClient) throws BusinessServiceException {
		// If any modules are excluded build a list of modules to include
		Set<String> exportModuleIds = null;
		if (excludedModuleIds != null && !excludedModuleIds.isEmpty()) {
			try {
				Set<String> allModules = snowOwlRestClient.eclQuery(branchPath, "<<" + Concepts.MODULE, 1000);
				allModules.removeAll(excludedModuleIds);
				exportModuleIds = new HashSet<>();
				exportModuleIds.addAll(allModules);
				logger.info("Excluded modules are {}, included modules are {} for release on {}", excludedModuleIds, exportModuleIds, branchPath);
			} catch (RestClientException e) {
				throw new BusinessServiceException("Failed to build list of modules for export.", e);
			}
		}
		return exportModuleIds;
	}


	private class ReleaseRunner implements Runnable {

		private final String branchPath;
		private final String effectiveDate;
		private final String productName;
		private final SnowOwlRestClient.ExportCategory exportCategory;
		private final OrchestrationCallback callback;
		private final Set<String> exportModuleIds;
		private String releaseCenter;
		private SnowOwlRestClient snowOwlRestClient;

		private ReleaseRunner(String productName, String releaseCenter, String branchPath, String effectiveDate, Set<String> exportModuleIds, 
				SnowOwlRestClient.ExportCategory exportCategory, SnowOwlRestClient snowOwlRestClient, OrchestrationCallback callback) {
			this.branchPath = branchPath;
			this.effectiveDate = effectiveDate;
			this.exportModuleIds = exportModuleIds;
			this.callback = callback;
			this.productName = productName;
			this.exportCategory = exportCategory;
			this.releaseCenter = releaseCenter;
			this.snowOwlRestClient = snowOwlRestClient;
		}

		@Override
		public void run() {
			OrchProcStatus finalOrchProcStatus = OrchProcStatus.FAILED;
			// Create files for SRS / Initiate SRS
			SRSProjectConfiguration config = new SRSProjectConfiguration(productName, this.releaseCenter, this.effectiveDate);
			config.setFailureExportMax(failureExportMax);
			File exportArchive  = null;
			try {
				// Export
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.EXPORTING.toString(), null);
				SnowOwlRestClient.ExportType exportType = flatIndexExportStyle ? SnowOwlRestClient.ExportType.SNAPSHOT : SnowOwlRestClient.ExportType.DELTA;
				
				exportArchive = snowOwlRestClient.export(branchPath, effectiveDate, exportModuleIds, exportCategory, exportType);
				
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.BUILD_INITIATING.toString(), null);
				srsClient.prepareSRSFiles(exportArchive, config);
				logger.info("RF2 delta files are extracted from the termServer export archive " + exportArchive.getName());
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
				
				if (callback != null) {
					callback.complete(finalOrchProcStatus);
				}
			} catch (Exception e) {
				processReportDAO.setStatus(branchPath, RELEASE_PROCESS, OrchProcStatus.FAILED.toString(), e.getMessage());
				logger.error("Release of {} failed.", branchPath, e);
			} finally {
				FileManager.deleteFileIfExists(exportArchive);
				FileManager.deleteFileIfExists(config.getInputFilesDir());
			}
		}
	}
}
