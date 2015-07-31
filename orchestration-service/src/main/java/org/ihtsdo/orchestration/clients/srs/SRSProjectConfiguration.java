package org.ihtsdo.orchestration.clients.srs;

import java.io.File;

public class SRSProjectConfiguration {

	File inputFilesDir;
	String releaseDate;

	public File getInputFilesDir() {
		return inputFilesDir;
	}

	public void setInputFilesDir(File archiveLocation) {
		this.inputFilesDir = archiveLocation;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}
}
