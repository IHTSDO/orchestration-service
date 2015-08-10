package org.ihtsdo.orchestration.utils;

import org.ihtsdo.otf.utils.DateUtils;
import org.junit.Assert;
import org.junit.Test;

public class DateUtilsTest {

	public static final String TEST_DATE = "20150324";
	public static final String EXPECTED_ISO = "2015-03-24";

	@Test
	public void testFormatAsISO() {
		String result = DateUtils.formatAsISO(TEST_DATE);
		Assert.assertEquals(EXPECTED_ISO, result);
	}

}
