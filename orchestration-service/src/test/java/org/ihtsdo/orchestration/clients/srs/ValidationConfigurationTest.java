package org.ihtsdo.orchestration.clients.srs;

import org.ihtsdo.orchestration.Config;
import org.ihtsdo.orchestration.TestProperties;
import org.ihtsdo.orchestration.clients.rvf.ValidationConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Config.class, TestProperties.class})
public class ValidationConfigurationTest {
	
	@Autowired ValidationConfiguration defaultConfig;
	
	@Test
	public void testValidationConfig() {
		Assert.assertNotNull(defaultConfig);
		ValidationConfiguration newConfig = ValidationConfiguration.copy(defaultConfig);
		Assert.assertEquals(newConfig, defaultConfig);
		
		newConfig.setAssertionGroupNames("CommonSnapshotValidation,IntSnapshotValidation");
		newConfig.setPreviousInternationalRelease("int_20160131");
		newConfig.setExtensionDependencyRelease("int_20160731");
		newConfig.setPreviousExtensionRelease("dk_20160331");
		newConfig.setReleaseDate("20160930");
		
		Assert.assertNotEquals(newConfig, defaultConfig);
		
		ValidationConfiguration copyOfNewConfig = ValidationConfiguration.copy(newConfig);
		Assert.assertEquals(newConfig, copyOfNewConfig);
	}

}
