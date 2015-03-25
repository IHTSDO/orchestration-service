package org.ihtsdo.ts.importer.clients.resty;

import us.monoid.json.JSONObject;
import us.monoid.web.AbstractContent;
import us.monoid.web.Content;
import us.monoid.web.Resty;

import java.io.UnsupportedEncodingException;

public class RestyHelper {

	public static final String UTF_8 = "UTF-8";

	public static Content content(JSONObject someJson, String aMimeType) {
		try {
			return new Content(aMimeType, someJson.toString().getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(UTF_8 + " encoding not supported!", e);
		}
	}

	public static AbstractContent putContent(JSONObject jsonObj, String aMimeType) {
		return Resty.put(content(jsonObj, aMimeType));
	}

	public static AbstractContent patchContent(JSONObject jsonObj, String aMimeType) {
		return Resty.patch(content(jsonObj, aMimeType));
	}
}
