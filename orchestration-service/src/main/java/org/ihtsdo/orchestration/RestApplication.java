package org.ihtsdo.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class RestApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Config.class, "server.port=9000");

		// Uncomment next line to trigger import on startup (useful for testing)
//		applicationContext.getBean(ImporterService.class).importCompletedWBContent(null, false);

//		applicationContext.getBean(TicketWorkflowManager.class).processIncompleteTickets();

	}

}
