package org.ihtsdo.orchestration.dao;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * File manager keeps track of the number of processes which are interested in a particular
 * file or directory, and delete it when that number drops to zero.
 */
@Component
public class FileManager {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	Map<File, Integer> fileMap = new HashMap<File, Integer>();
	
	public void addProcess(File file) {
		if (fileMap.containsKey(file)) {
			int currentValue = fileMap.get(file).intValue();
			fileMap.put(file, new Integer(currentValue + 1));
		}	else {
			fileMap.put(file, new Integer(1));
		}
	}
	
	public void removeProcess(File file) {
		if	(file != null) {
			if	(fileMap.containsKey(file)) {
				int currentValue = fileMap.get(file);
				if (currentValue > 1) {
					fileMap.put(file, currentValue - 1);
					return;
				} else {
					fileMap.remove(file);
				}
			}
			// delete if the file exists
			deleteFileIfExists(file);
		}
	}

	private void deleteFileIfExists(File file) {
		logger.debug("Removing " + file.getAbsolutePath());
		if	(file.exists()) {
			if (file.isDirectory()) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					logger.error("Failed to delete directory {}", file.getAbsolutePath(), e);
				}
			}	else {
				file.delete();
			}
		}
	}
}
