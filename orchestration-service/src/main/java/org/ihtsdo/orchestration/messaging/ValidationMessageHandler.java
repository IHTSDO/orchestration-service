package org.ihtsdo.orchestration.messaging;

import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.ASSERTION_GROUP_NAMES;
import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.EXTENSION_DEPENDENCY_RELEASE;
import static org.ihtsdo.orchestration.rest.ValidationParameterConstants.PREVIOUS_EXTENSION_RELEASE;

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
	private ValidationConfiguration defaultValidationConfig;

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
		ValidationConfiguration validationConfig = defaultValidationConfig.clone();
		String assertionGroups = messageIn.getStringProperty(ASSERTION_GROUP_NAMES);
		if ( assertionGroups != null) {
			validationConfig.setAssertionGroupNames(assertionGroups);
		}
		String previousExtension = messageIn.getStringProperty(PREVIOUS_EXTENSION_RELEASE);
		if (previousExtension != null) {
			validationConfig.setPreviousExtensionRelease(previousExtension);
		}
		String extensionDependencyRelease = messageIn.getStringProperty(EXTENSION_DEPENDENCY_RELEASE);
		if (extensionDependencyRelease != null) {
			validationConfig.setExentsionDependencyRelease(extensionDependencyRelease);
		}
		return validationConfig;
	}


}
