package org.ihtsdo.ts.importer;

import org.springframework.boot.SpringApplication;

public class RestApplication {

	public static void main(String[] args) {
		SpringApplication.run(Config.class, "server.port=9000");
	}

}
