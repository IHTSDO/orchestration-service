package org.ihtsdo.orchestration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableSwagger2
@ImportResource("classpath:spring_context.xml")
public class Config {
	
	@Bean
	public Docket newsApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("default")
				.apiInfo(apiInfo())
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any())
				.build(); 
	}
	
	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title("IHTSDO Orchestration - Swagger")
				.description("IHTSDO Orchestration - Swagger")
				.contact("techsupport@ihtsdo.org")
				.license("Apache License Version 2.0")
				.licenseUrl("https://ihtsdo.org")
				.version("2.0")
				.build();
	}

}
