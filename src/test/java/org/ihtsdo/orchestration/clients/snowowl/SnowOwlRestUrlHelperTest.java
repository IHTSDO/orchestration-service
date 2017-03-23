package org.ihtsdo.orchestration.clients.snowowl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SnowOwlRestUrlHelperTest {

	private SnowOwlRestUrlHelper urlHelper;

	@Before
	public void setup() {
		urlHelper = new SnowOwlRestUrlHelper("http://localhost:8080/snowowl/");
	}

	@Test
	public void testGetBranchPath() throws Exception {
		Assert.assertEquals("MAIN", urlHelper.getMainBranchPath());
		Assert.assertEquals("MAIN/projectA", urlHelper.getBranchPath("projectA"));
		Assert.assertEquals("MAIN/projectA", urlHelper.getBranchPath("projectA", null));
		Assert.assertEquals("MAIN/projectA/task1", urlHelper.getBranchPath("projectA", "task1"));
	}

	@Test
	public void testGetBranchChildrenUrl() throws Exception {
		Assert.assertEquals("http://localhost:8080/snowowl/snomed-ct/v2/branches/MAIN/children", urlHelper.getBranchChildrenUrl("MAIN"));
		Assert.assertEquals("http://localhost:8080/snowowl/snomed-ct/v2/branches/MAIN/projectA/children", urlHelper.getBranchChildrenUrl("MAIN/projectA"));
	}

	@Test
	public void testRemoveTrailingSlash() throws Exception {
		Assert.assertEquals("http://localhost:8080/snowowl", SnowOwlRestUrlHelper.removeTrailingSlash("http://localhost:8080/snowowl"));
		Assert.assertEquals("http://localhost:8080/snowowl", SnowOwlRestUrlHelper.removeTrailingSlash("http://localhost:8080/snowowl/"));
	}
}