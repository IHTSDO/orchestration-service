package org.ihtsdo.srs.client;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({ "file:src/main/resources/ApplicationContext.xml" })
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

}
