package org.ihtsdo.orchestration.importer.clients.snowowl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ihtsdo.orchestration.clients.snowowl.ImportError;
import org.ihtsdo.orchestration.clients.snowowl.ImportErrorParser;
import org.ihtsdo.orchestration.clients.snowowl.ImportErrorParserException;

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

		Assert.assertEquals(295, importErrors.size());

		int errorIndex = 0;
		for (ImportError importError : importErrors) {
			String importDetails = "Index: " + errorIndex + ", message:" + importError.getMessage();
			Assert.assertNotNull("Message should not be null. " + importDetails, importError.getMessage());
			String conceptId = importError.getConceptId();
			Assert.assertNotNull("Concept id should not be null. " + importDetails, conceptId);
			Assert.assertNotEquals("Concept id should not be empty. " + importDetails, "", conceptId);
			Assert.assertTrue("Concept id should only contain numbers. " + importDetails, conceptId.matches("^[\\d]*$"));
			Assert.assertTrue("Concept id length should be 9-17. " + importDetails, conceptId.length() >= 7 && conceptId.length() <= 17);
			errorIndex++;
		}

		assertTypeAndConceptId(importErrors.get(0), "713176007");
		assertTypeAndConceptId(importErrors.get(13), "713513004");
		assertTypeAndConceptId(importErrors.get(294), "713962005");
	}

	private void assertTypeAndConceptId(ImportError importError, String expectedConceptId) {
		Assert.assertEquals(expectedConceptId, importError.getConceptId());
	}

}
