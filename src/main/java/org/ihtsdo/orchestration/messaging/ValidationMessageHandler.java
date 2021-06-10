package org.ihtsdo.orchestration.messaging;

import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.*;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
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
	private static final String X_AUTH_TOKEN = "X-AUTH-token";

	@Autowired
	private ValidationService validationService;

	@Autowired
	private MessagingHelper messagingHelper;
	
	@Autowired
	private String failureExportMax;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@JmsListener(destination = "${orchestration.name}.orchestration.termserver-release-validation")
	public void receiveValidationRequest(final TextMessage messageIn) {
		try {
			ValidationConfiguration validationConfig = constructValidationConfig(messageIn);
			String authToken = messageIn.getStringProperty(X_AUTH_TOKEN);
			validationService.validate(validationConfig, messageIn.getStringProperty(PATH), messageIn.getStringProperty(EFFECTIVE_TIME), authToken, finalValidationStatus -> {
				Map<String, Object> properties = new HashMap<>();
				properties.put("status", finalValidationStatus.getStatus().toString());
				properties.put("reportUrl", finalValidationStatus.getReportUrl());
				properties.put(X_AUTH_TOKEN, authToken);
				messagingHelper.sendResponse(messageIn, "", properties);
			});
		} catch (JMSException | EntityAlreadyExistsException e) {
			logger.error("Failed to handle message, responding with error.", e);
			messagingHelper.sendErrorResponse(messageIn, e);
		}
	}

	private ValidationConfiguration constructValidationConfig(final TextMessage messageIn) throws JMSException {
		ValidationConfiguration validationConfig = new ValidationConfiguration();
		validationConfig.setFailureExportMax(failureExportMax);
		validationConfig.setAssertionGroupNames(messageIn.getStringProperty(ASSERTION_GROUP_NAMES));
		validationConfig.setPreviousPackage(messageIn.getStringProperty(PREVIOUS_PACKAGE));
		validationConfig.setDependencyPackage(messageIn.getStringProperty(DEPENDENCY_PACKAGE));
		validationConfig.setPreviousRelease(messageIn.getStringProperty(PREVIOUS_RELEASE));
		validationConfig.setRvfDroolsAssertionGroupNames(messageIn.getStringProperty(RVF_DROOLS_ASSERTION_GROUP_NAMES));
		validationConfig.setIncludedModuleIds(messageIn.getStringProperty(DEFAULT_MODULE_ID));
		String dependencyRelease = messageIn.getStringProperty(DEPENDENCY_RELEASE);
		if (dependencyRelease != null) {
			validationConfig.setReleaseCenter(messageIn.getStringProperty(SHORT_NAME));
			validationConfig.setDependencyRelease(dependencyRelease);
		} else {
			validationConfig.setReleaseCenter(INTERNATIONAL);
		}
		String enableMRCMValidation = messageIn.getStringProperty(ENABLE_MRCM_VALIDATION);
		if (enableMRCMValidation != null) {
			validationConfig.setEnableMRCMValidation(Boolean.parseBoolean(enableMRCMValidation));
		}
		else {
			validationConfig.setEnableMRCMValidation(false);
		}

		logger.info("Validation config created:{}", validationConfig);
		return validationConfig;
	}
}
