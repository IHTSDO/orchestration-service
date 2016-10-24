package org.ihtsdo.orchestration.clients.srs;

import org.ihtsdo.orchestration.Config;
import org.ihtsdo.orchestration.TestProperties;
import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Config.class, TestProperties.class})
public class ValidationConfigurationTestIntegration {
	
	private static final String COMMON_AUTHORING = "common-authoring";
	
	@Test
	public void testCheckConfigWithNothingSet() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		Assert.assertEquals(config.checkMissingParameters(),"assertionGroupNames can't be null.previousInternationalRelease,extensionDependencyRelease and previousExtensionRelease can't be all null.");
	}
	
	
	@Test
	public void testWithNoExtensionConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		config.setPreviousExtensionRelease("dk-20160215");
		String errorMsg = "previousExtensionRelease and extensionDependencyRelease can't be null for extension validation.";
		Assert.assertEquals(errorMsg, config.checkMissingParameters());
		config.setExtensionDependencyRelease("int_20160131");
		config.setPreviousExtensionRelease(null);
		Assert.assertEquals(errorMsg, config.checkMissingParameters());
		
	}

	@Test
	public void testSuccessfulWithExtConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		config.setPreviousExtensionRelease("dk-20160215");
		config.setExtensionDependencyRelease("int_20160131");
		Assert.assertEquals(null, config.checkMissingParameters());
		
	}
	
	@Test
	public void testSuccessfulWithIntConfig() {
		ValidationConfiguration config = new ValidationConfiguration();
		Assert.assertNotNull(config.checkMissingParameters());
		config.setAssertionGroupNames(COMMON_AUTHORING);
		config.setPreviousInternationalRelease("int_20160131");
		Assert.assertEquals(null, config.checkMissingParameters());
		
	}
}
