package org.ihtsdo.ts.importer.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/REST")
public class RootController {

	@RequestMapping
	public Map<String, String> getAvailableEndpoints(HttpServletRequest request) {
		String requestURL = request.getRequestURL().append("/").toString();
		System.out.println(requestURL);
		HashMap<String, String> endpoints = new HashMap<>();
		addEndpoint(requestURL, endpoints, "backlog");
		return endpoints;
	}

	private String addEndpoint(String requestURI, HashMap<String, String> endpoints, String endpoint) {
		return endpoints.put(endpoint, requestURI + endpoint);
	}

}
