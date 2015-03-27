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
			Assert.assertNotNull("Concept id should not be null. Index: " + errorIndex, importError.getType());
			String conceptId = importError.getConceptId();
			Assert.assertNotNull("Concept id should not be null. Index: " + errorIndex, conceptId);
			Assert.assertNotEquals("Concept id should not be empty. Index: " + errorIndex, "", conceptId);
			Assert.assertTrue("Concept id should only contain numbers. Index: " + errorIndex, conceptId.matches("^[\\d]*$"));
			Assert.assertTrue("Concept id length should be 9-17. Index: " + errorIndex, conceptId.length() >= 7 && conceptId.length() <= 17);
			errorIndex++;
		}

		assertTypeAndConceptId(importErrors.get(0), ImportError.ImportErrorType.DESCRIPTION_IDENTIFIER_NOT_UNIQUE, "700000001");
		assertTypeAndConceptId(importErrors.get(13), ImportError.ImportErrorType.DESCRIPTION_CONCEPT_DOES_NOT_EXIST, "10676111000119102");
		assertTypeAndConceptId(importErrors.get(96), ImportError.ImportErrorType.RELATIONSHIP_DESTINATION_CONCEPT_DOES_NOT_EXIST, "707142008");
		assertTypeAndConceptId(importErrors.get(130), ImportError.ImportErrorType.CONCEPT_INACTIVATION_WHEN_REFERENCED_IN_ACTIVE_RELATIONSHIPS, "214178009");
		assertTypeAndConceptId(importErrors.get(130), ImportError.ImportErrorType.CONCEPT_INACTIVATION_WHEN_REFERENCED_IN_ACTIVE_RELATIONSHIPS, "214178009");
		assertTypeAndConceptId(importErrors.get(337), ImportError.ImportErrorType.RELATIONSHIP_SOURCE_CONCEPT_DOES_NOT_EXIST, "707548000");
	}

	private void assertTypeAndConceptId(ImportError importError, ImportError.ImportErrorType expectedType, String expectedConceptId) {
		Assert.assertEquals(expectedType, importError.getType());
		Assert.assertEquals(expectedConceptId, importError.getConceptId());
	}

}
