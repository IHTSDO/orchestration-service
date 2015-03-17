package org.ihtsdo.srs.client;

import java.io.File;

import com.google.common.io.Files;

public class SRSRestClientHelper {

	/*
	 * @return - the directory containing the files ready for uploading to SRS
	 */
	public static File readyInputFiles(File archive) {

		// Extract the archive into a temp directory
		File extractDir = Files.createTempDir();

		// Merge the refsets into the expected files and replace any "unpublished" dates
		// with today's date

		return extractDir;
	}

}
