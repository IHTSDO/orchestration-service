package org.ihtsdo.ts.importer.clients.resty;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.AbstractContent;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.Resty.Option;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestyHelper extends Resty {

	public static final String UTF_8 = "UTF-8";
	private static final Logger LOGGER = LoggerFactory.getLogger(RestyHelper.class);

	public RestyHelper(Option option) {
		super(option);
	}

	public RestyHelper() {
		super();
	}

	public static Content content(JSONObject someJson, String aMimeType) {
		try {
			return new Content(aMimeType, someJson.toString().getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(UTF_8 + " encoding not supported!", e);
		}
	}

	public JSONResource put(String url, JSONObject jsonObj, String contentType) throws IOException, JSONException {
		return json(url, put(content(jsonObj, contentType)));

	}

	public JSONResource json(String url, JSONObject jsonObj, String contentType) throws IOException, JSONException {
		return json(url, content(jsonObj, contentType));

	}

	public JSONResource json(String url, AbstractContent content) throws IOException, JSONException {
		JSONResource response = super.json(url, content);
		String statusCode = response.getUrlConnection().getHeaderField("Status-Code");
		if (statusCode != null && !statusCode.startsWith("2")) {
			throw new IOException("Call to " + url + " returned status " + statusCode + " and body " + response.object().toString(2));
		} else {
			LOGGER.debug("Call to " + url + " returned headers: ");
			for (Entry<String, List<String>> header : response.getUrlConnection().getHeaderFields().entrySet()) {
				LOGGER.debug(header.getKey());
				for (String item : header.getValue()) {
					LOGGER.debug("\t" + item);
				}
			}
		}
		return response;
	}
}
