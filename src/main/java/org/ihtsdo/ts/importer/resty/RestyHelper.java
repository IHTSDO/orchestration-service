package org.ihtsdo.ts.importer.resty;

import us.monoid.json.JSONObject;
import us.monoid.web.Content;

import java.io.UnsupportedEncodingException;

public class RestyHelper {

	public static Content content(JSONObject someJson, String aMimeType) {
		Content c = null;
		try {
			c = new Content(aMimeType, someJson.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) { /* UTF-8 is never unsupported */
		}
		return c;
	}
}
