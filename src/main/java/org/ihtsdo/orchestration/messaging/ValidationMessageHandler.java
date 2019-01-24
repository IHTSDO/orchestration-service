package org.ihtsdo.orchestration.messaging;

import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.*;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.ihtsdo.orchestration.service.OrchProcStatus;
import org.ihtsdo.orchestration.service.OrchestrationCallback;
import org.ihtsdo.orchestration.service.ValidationService;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ValidationMessageHandler {

	public static final String PATH = "path";
	public static final String EFFECTIVE_TIME = "effective-time";

	@Autowired
	private ValidationService validationService;

	@Autowired
	private MessagingHelper messagingHelper;
	
	@Autowired
	private String failureExportMax;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	@JmsListener(destination = "${orchestration.name}.orchestration.termserver-release-validation")
	public void receiveValidationRequest(final TextMessage messageIn) {
		try {
			ValidationConfiguration validationConfig = constructValidaitonConfig(messageIn);
			validationService.validate(validationConfig,messageIn.getStringProperty(PATH), messageIn.getStringProperty(EFFECTIVE_TIME), new OrchestrationCallback() {
				@Override
						public void complete(OrchProcStatus finalValidationStatus) {
					Map<String, String> properties = new HashMap<>();
					properties.put("status", finalValidationStatus.toString());
					messagingHelper.sendResponse(messageIn, "", properties);
				}
			});
		} catch (JMSException | EntityAlreadyExistsException e) {
			logger.error("Failed to handle message, responding with error.", e);
			messagingHelper.sendErrorResponse(messageIn, e);
		}
	}

	private ValidationConfiguration constructValidaitonConfig(final TextMessage messageIn)
			throws JMSException {
		ValidationConfiguration validationConfig = new ValidationConfiguration();
		validationConfig.setFailureExportMax(failureExportMax);
		String assertionGroups = messageIn.getStringProperty(ASSERTION_GROUP_NAMES);
		if ( assertionGroups != null) {
			validationConfig.setAssertionGroupNames(assertionGroups);
		}
	
		String codeSystemName = messageIn.getStringProperty(CODE_SYSTEM_SHORT_NAME);
		String previousPackages = messageIn.getStringProperty(PREVIOUS_PACKAGES);
		if (previousPackages != null) {
			String [] releases = previousPackages.split(",", -1);
			if (releases.length > 1) {
				//extension
				for (String release : releases) {
					if (release.toLowerCase().contains(codeSystemName.toLowerCase())) {
						validationConfig.setPreviousRelease(release);
					} else {
						validationConfig.setDependencyRelease(release);
					}
				}
			} else {
				validationConfig.setPreviousRelease(previousPackages);
			}
		}
		String extensionDependencyRelease = messageIn.getStringProperty(DEPENDENCY_RELEASE);
		String productShortName = messageIn.getStringProperty(SHORT_NAME);
	
		if (extensionDependencyRelease != null) {
			validationConfig.setReleaseCenter(productShortName);
		} else {
			validationConfig.setReleaseCenter(INTERNATIONAL);
		}
		logger.info("Validation conifg created:" + validationConfig);
		return validationConfig;
	}


}
