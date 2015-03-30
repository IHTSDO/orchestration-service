package org.ihtsdo.ts.importer.clients.snowowl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ImportErrorParserTest {

	private ImportErrorParser importErrorParser;

	@Before
	public void setUp() throws Exception {
		importErrorParser = new ImportErrorParser();
	}

	@Test
	public void test() throws ImportErrorParserException {
		List<ImportError> importErrors = importErrorParser.parseLogForLatestImportErrors(null, getClass().getResourceAsStream("/import.log"));

		Assert.assertEquals(351, importErrors.size());

		int errorIndex = 0;
		for (ImportError importError : importErrors) {
			Assert.assertNotNull("Message should not be null. Index: " + errorIndex, importError.getMessage());
			String conceptId = importError.getConceptId();
			Assert.assertNotNull("Concept id should not be null. Index: " + errorIndex, conceptId);
			Assert.assertNotEquals("Concept id should not be empty. Index: " + errorIndex, "", conceptId);
			Assert.assertTrue("Concept id should only contain numbers. Index: " + errorIndex, conceptId.matches("^[\\d]*$"));
			Assert.assertTrue("Concept id length should be 9-17. Index: " + errorIndex, conceptId.length() >= 7 && conceptId.length() <= 17);
			errorIndex++;
		}

		assertTypeAndConceptId(importErrors.get(0), "700000001");
		assertTypeAndConceptId(importErrors.get(13), "10676111000119102");
		assertTypeAndConceptId(importErrors.get(96), "707142008");
		assertTypeAndConceptId(importErrors.get(130), "214178009");
		assertTypeAndConceptId(importErrors.get(130), "214178009");
		assertTypeAndConceptId(importErrors.get(337), "707548000");
	}

	private void assertTypeAndConceptId(ImportError importError, String expectedConceptId) {
		Assert.assertEquals(expectedConceptId, importError.getConceptId());
	}

}
