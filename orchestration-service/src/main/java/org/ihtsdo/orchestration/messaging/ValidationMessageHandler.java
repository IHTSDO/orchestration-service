package org.ihtsdo.orchestration.messaging;

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

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Component
public class ValidationMessageHandler {

	public static final String PATH = "path";
	public static final String EFFECTIVE_TIME = "effective-time";

	@Autowired
	private ValidationService validationService;

	@Autowired
	private MessagingHelper messagingHelper;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@JmsListener(destination = "orchestration.termserver-release-validation")
	public void receiveValidationRequest(final TextMessage messageIn) {
		try {
			validationService.validate(messageIn.getStringProperty(PATH), messageIn.getStringProperty(EFFECTIVE_TIME), new OrchestrationCallback() {
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


}
