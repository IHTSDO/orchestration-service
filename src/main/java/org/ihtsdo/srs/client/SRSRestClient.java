package org.ihtsdo.srs.client;

import java.io.File;
import java.net.URLConnection;

import us.monoid.web.Resty;
import us.monoid.web.RestyMod;

public class SRSRestClient {

	protected static final String SRS_CONTENT_TYPE = null;

	private final String srsURL;

	private final RestyMod resty;

	public SRSRestClient(String srsURL, String username, String password) {
		this.srsURL = srsURL;
		this.resty = new RestyMod(new Resty.Option() {
			@Override
			public void apply(URLConnection aConnection) {
				aConnection.addRequestProperty("Accept", SRS_CONTENT_TYPE);
			}
		});
		resty.authenticate(srsURL, username, password.toCharArray());
	}

	public static void runDailyBuild(File srsFilesDir, String releaseDate) {
		// TODO Auto-generated method stub

	}

}
