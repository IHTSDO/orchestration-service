package org.ihtsdo.srs.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SRSRestClientHelperTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static String TEST_DATE = "20150318";
	private static String TEST_ARCHIVE = "test_archive_20150318.zip";
	File testArchive = null;

	@Before
	public void setUp() throws Exception {
		URL testArchiveURL = getClass().getResource(TEST_ARCHIVE);
		testArchive = new File(testArchiveURL.getFile());
		if (!testArchive.exists()) {
			throw new Exception("Unable to load test resource: " + TEST_ARCHIVE);
		}
	}

	@Test
	public void testReleaseDateRecovery() throws ProcessWorkflowException {
		String testFileName = "der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20150318.txt";
		String actualDate = SRSRestClientHelper.findDateInString(testFileName);
		Assert.assertEquals(TEST_DATE, actualDate);
	}

	@Test
	public void testRecoverDate() throws Exception {
		String releaseDate = SRSRestClientHelper.recoverReleaseDate(testArchive);
		Assert.assertEquals(TEST_DATE, releaseDate);
	}

	@Test
	public void testPrepareFiles() throws ProcessWorkflowException, IOException {
		File location = SRSRestClientHelper.readyInputFiles(testArchive, TEST_DATE);
		logger.debug("Test archive made ready for SRS input at {}", location.getAbsolutePath());
		// logger.debug("Tidying up folder at {}", location.getAbsolutePath());
		// FileUtils.deleteDirectory(location);
	}


}
