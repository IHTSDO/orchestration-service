package org.ihtsdo.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RestApplication {
	
	private static ConfigurableApplicationContext applicationContext;

	public static void main(String[] args) throws Exception {
		applicationContext = SpringApplication.run(Config.class, "server.port=9000");

		// Uncomment next line to trigger import on startup (useful for testing)
//		applicationContext.getBean(ImporterService.class).importCompletedWBContent(null, true);
//		applicationContext.getBean(TicketWorkflowManager.class).processIncompleteTickets();
		
		// Method to test validation on startup
//		testValidation();
	}

/*	private static void testValidation() throws BeansException, EntityAlreadyExistsException {
		ValidationConfiguration config = new ValidationConfiguration();
		config.setAssertionGroupNames("file-centric-validation");
		config.setPreviousInternationalRelease("int_20170131");
		String branchPath = "MAIN/CMTMU";
		String effectiveDate = "20170731";
		applicationContext.getBean(ValidationService.class).validate(config, branchPath, effectiveDate, null);
	}*/

}
