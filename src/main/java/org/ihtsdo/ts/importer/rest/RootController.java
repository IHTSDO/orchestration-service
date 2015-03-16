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
		HashMap<String, String> endpoints = new HashMap<>();
		String requestURL = addTrailingSlash(request.getRequestURL().toString());
		addEndpoint(requestURL, endpoints, "backlog");
		addEndpoint(requestURL, endpoints, "selections/create");
		return endpoints;
	}

	private String addTrailingSlash(String requestURL) {
		if (requestURL.charAt(requestURL.length() - 1) != '/') {
			requestURL += "/";
		}
		return requestURL;
	}

	private String addEndpoint(String requestURI, HashMap<String, String> endpoints, String endpoint) {
		return endpoints.put(endpoint, requestURI + endpoint);
	}

}
