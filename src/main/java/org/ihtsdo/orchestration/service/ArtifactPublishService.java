package org.ihtsdo.orchestration.service;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.ihtsdo.orchestration.dao.FileManager;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This service makes data such as delta exports available on a JMS Topic
 * to any process that might be interested in consuming it eg RVF, ADS
 * eg dev-int.orchestration.termserver-exported-delta
 */
@Service
public class ArtifactPublishService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	MessagingHelper messenger;
	
	@Autowired
	FileManager fileManager;
	
	@Value("${orchestration.name}")
	String platform; //Combination of environment plus audience eg dev-int
	
	@Value("${orchestration.jms.timeToLive}")
	int timeToLive; //seconds
	
	public void publish (File archive, String source, Map<String, ? extends Object> messageProperties) {
		DataPublisher dp = new DataPublisher(archive, source, messageProperties);
		fileManager.addProcess(archive);
		new Thread(dp).start();
	}

	private class DataPublisher implements Runnable {
		File archive;
		String source;
		Map<String, ? extends Object> messageProperties;
		
		DataPublisher (File archive, String source, Map<String, ? extends Object> messageProperties) {
			this.archive = archive;
			this.source = source;
			this.messageProperties = messageProperties;
		}

		@Override
		public void run() {
			String dest = platform + ".orchestration." + source;
			try {
				String base64Archive = DatatypeConverter.printBase64Binary(Files.readAllBytes(archive.toPath()));
				logger.debug("Encoded archive to {}Kb",(base64Archive.length() / 1024));
				messenger.publish(dest, base64Archive, messageProperties, timeToLive);
			} catch (Exception e) {
				logger.error("Failed to publish archive to {} with properties {}", dest, messageProperties, e);
			} finally {
				fileManager.removeProcess(archive);
			}
		}
	}
}
