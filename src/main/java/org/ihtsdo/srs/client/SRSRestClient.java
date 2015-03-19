package org.ihtsdo.srs.client;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.util.Assert;

import us.monoid.web.Resty;
import us.monoid.web.RestyMod;

public class SRSRestClient {

	protected static final String SRS_CONTENT_TYPE = "*/*";

	private static String BLANK_MANIFEST = "/manifest_no_date.xml";
	private static String DATE_MARKER = "########";

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

	public SRSRestClient() {
		srsURL = null;
		resty = null;
	}

	public void runDailyBuild(File srsFilesDir, String releaseDate) throws IOException {

		// Lets upload the manifest first
		File configuredManifest = configureManifest(releaseDate);

	}

	protected File configureManifest(String releaseDate) throws IOException {
		// We need to build a manifest file containing the target release date
		File blankManifest = new File(getClass().getResource(BLANK_MANIFEST).getPath());
		Assert.isTrue(blankManifest.exists(), "Failed to load blank manifest");
		String content = new String(Files.readAllBytes(blankManifest.toPath()), StandardCharsets.UTF_8);
		content = content.replaceAll(DATE_MARKER, releaseDate);
		File configuredManifest = File.createTempFile("manifest_" + releaseDate, ".xml");
		Files.write(configuredManifest.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return configuredManifest;
	}

}
