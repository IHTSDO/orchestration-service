package org.ihtsdo.orchestration.clients.srs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.ihtsdo.orchestration.clients.srs.SRSRestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.RestyMod;

@RunWith(SpringJUnit4ClassRunner.class)
public class SRSRestClientTest {

	private SRSRestClient srs;

	private static String TEST_DATE = "20990101";

	@Before
	public void setUp() throws Exception {
		this.srs = new SRSRestClient();
	}

	@Test
	public void testConfigureManifest() throws IOException {
		File configuredManifest = srs.configureManifest(TEST_DATE);
		Assert.assertTrue(configuredManifest.exists());
		configuredManifest.delete();
	}

	@Test
	public void testItemsOfInterest() throws Exception {

		// Use Resty to open a local resource
		Resty resty = new RestyMod();
		final URL resourceUrl = getClass().getResource("sample_trigger_response.json");
		// logger.debug("Loading resource: {}", resourceUrl.toString());
		JSONResource responseJSON = resty.json(resourceUrl.toString());

		Map<String, String> ioi = srs.recoverItemsOfInterest(responseJSON);
		// logger.debug("Recovered Items of Interest Map: {}", MapUtils.toString(ioi));
		Assert.assertEquals(SRSRestClient.ITEMS_OF_INTEREST.length, ioi.size());
	}

}
