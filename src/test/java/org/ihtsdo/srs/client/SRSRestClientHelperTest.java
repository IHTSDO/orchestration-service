package org.ihtsdo.srs.client;

import java.io.File;
import java.io.IOException;

import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SRSRestClientHelperTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static String TEST_DATE = "20150318";

	@Test
	public void testReleaseDateRecovery() throws ProcessWorkflowException {
		String testFileName = "der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20150318.txt";
		String actualDate = SRSRestClientHelper.findDateInString(testFileName);
		Assert.assertEquals(TEST_DATE, actualDate);
	}

	@Test
	public void testRecoverDate() throws ProcessWorkflowException, IOException {
		File exportedArchive = new File("/var/folders/zn/fmx3y4695q9d3rw76prt165m0000gn/T/ts-extract9102211379737573377.zip");
		if (exportedArchive.exists()) {
			String releaseDate = SRSRestClientHelper.recoverReleaseDate(exportedArchive);
			Assert.assertEquals(TEST_DATE, releaseDate);
		}
	}

	@Test
	public void testPrepareFiles() throws ProcessWorkflowException, IOException {
		File exportedArchive = new File("/var/folders/zn/fmx3y4695q9d3rw76prt165m0000gn/T/ts-extract9102211379737573377.zip");
		if (exportedArchive.exists()) {
			File location = SRSRestClientHelper.readyInputFiles(exportedArchive, TEST_DATE);
			logger.debug("Test archive made ready for SRS input at {}", location.getAbsolutePath());
		}
	}


}
