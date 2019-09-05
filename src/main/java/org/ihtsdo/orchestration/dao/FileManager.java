package org.ihtsdo.orchestration.dao;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * File manager keeps track of the number of processes which are interested in a particular
 * file or directory, and delete it when that number drops to zero.
 */

public class FileManager {
	
	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
	
	public static void deleteFileIfExists(File file) {
		logger.debug("Removing " + file.getAbsolutePath());
		if (file.exists()) {
			if (file.isDirectory()) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					logger.error("Failed to delete directory {}", file.getAbsolutePath(), e);
				}
			} else {
				file.delete();
			}
		}
	}
}
