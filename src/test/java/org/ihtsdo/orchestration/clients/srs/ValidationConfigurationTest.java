package org.ihtsdo.orchestration.clients.srs;

import static org.junit.Assert.*;

import org.ihtsdo.orchestration.Config;
import org.ihtsdo.orchestration.TestProperties;
import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ValidationConfigurationTest {
	
	private static final String COMMON_AUTHORING = "common-authoring";
	
	@Test
	public void testCheckConfigWithNothingSet() {
		ValidationConfiguration config = new ValidationConfiguration();
		assertNotNull(config.checkMissingParameters());
		assertEquals("assertionGroupNames can't be null. previousRelease and dependencyRelease can't be both null. previousPackage and dependencyPackage can't be both null.",
				config.checkMissingParameters());
	}
	
	
	@Test
	public void testWithNoExtensionConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		String errorMsg = "previousRelease and dependencyRelease can't be both null. previousPackage and dependencyPackage can't be both null.";
		assertEquals(errorMsg, config.checkMissingParameters());
		config.setDependencyRelease("20160131");
		config.setPreviousRelease("20160215");
		config.setDependencyPackage("SnomedCT_INT_20160131.zip");
		config.setDependencyPackage("SnomedCT_DK_201602151.zip");
		assertNull(config.checkMissingParameters());
	}

	@Test
	public void testSuccessfulWithExtConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		config.setPreviousRelease("dk-20160215");
		config.setDependencyRelease("int_20160131");
		assertEquals("previousPackage and dependencyPackage can't be both null.", config.checkMissingParameters());
	}
	
	@Test
	public void testSuccessfulWithIntConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		config.setPreviousRelease("20160131");
		config.setPreviousPackage("SnomedCT_INT_20160131.zip");
		assertEquals(null, config.checkMissingParameters());
	}
}
