package org.ihtsdo.ts.importer.clients.snowowl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImportErrorParser {

	public static final String UTF_8 = "UTF-8";
	public static final String IMPORT_STARTED_FROM_RF2_RELEASE_FORMAT = "import started from RF2 release format";
	public static final String IMPORT_UTIL = " com.b2international.snowowl.snomed.importer.rf2.util.ImportUtil ";
	public static final String ERROR = " ERROR ";
	public static final String CONCEPT_ID = "concept ID ";
	public static final String SOURCE_CONCEPT = "Source concept '";
	public static final String DESCRIPTION_IDENTIFIER_IS_NOT_UNIQUE = "Description identifier is not unique ";
	public static final String DESCRIPTION_REFERS_TO_A_NON_EXISTING_CONCEPT = "Description refers to a non-existing concept ";
	public static final String RELATIONSHIP_REFERS_TO_A_NON_EXISTING_CONCEPT_IN_COLUMN_DESTINATION_ID = "Relationship refers to a non-existing concept in column 'destinationId' ";
	public static final String RELATIONSHIP_REFERS_TO_A_NON_EXISTING_CONCEPT_IN_COLUMN_SOURCE_ID = "Relationship refers to a non-existing concept in column 'sourceId' ";
	public static final String THE_CONCEPTS_BELOW_ARE_REFERENCED_IN_ACTIVE_RELATIONSHIPS = "The concepts below are referenced in active relationships. ";
	public static final String VALIDATION_ENCOUNTERED = "Validation encountered ";

	private Logger logger = LoggerFactory.getLogger(getClass());

	public List<ImportError> parseLogForLatestImportErrors(InputStream snowOwlRolloverLogStream, InputStream snowOwlLogStream) throws ImportErrorParserException {
		Path tempFile;
		try {
			tempFile = Files.createTempFile(null, null);
		} catch (IOException e) {
			throw new ImportErrorParserException("Failed to create temp file", e);
		}

		long importStartOffset = copyLogAndGetImportStartOffset(snowOwlRolloverLogStream, snowOwlLogStream, tempFile);
		if (importStartOffset == -1) {
			throw new ImportErrorParserException("No import found in the log.");
		}

		try {
			List<ImportError> importErrors = new ArrayList<>();
			try (BufferedReader reader = Files.newBufferedReader(tempFile, Charset.forName(UTF_8))) {
				discardLinesBeforeImportStart(reader, importStartOffset);
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains(IMPORT_UTIL) && line.contains(ERROR)) {
						ImportError importError = parseErrorLine(line);
						if (importError != null) {
							importErrors.add(importError);
						}
					}
				}
			}
			return importErrors;
		} catch (IOException e) {
			throw new ImportErrorParserException("Failed to read temp file.", e);
		}
	}

	private ImportError parseErrorLine(String importErrorLogLine) {
		ImportError importError;
		String message = importErrorLogLine.substring(importErrorLogLine.indexOf(IMPORT_UTIL) + IMPORT_UTIL.length()).trim();
		logger.debug("Error message [{}]", message);
		if (message.startsWith(VALIDATION_ENCOUNTERED)) {
			importError = null;
		} else if (message.startsWith(DESCRIPTION_IDENTIFIER_IS_NOT_UNIQUE)) {
			importError = new ImportError(ImportError.ImportErrorType.DESCRIPTION_IDENTIFIER_NOT_UNIQUE, getConceptIdFromEndOfLine(message));
		} else if (message.startsWith(DESCRIPTION_REFERS_TO_A_NON_EXISTING_CONCEPT)) {
			importError = new ImportError(ImportError.ImportErrorType.DESCRIPTION_CONCEPT_DOES_NOT_EXIST, getConceptIdFromEndOfLine(message));
		} else if (message.startsWith(RELATIONSHIP_REFERS_TO_A_NON_EXISTING_CONCEPT_IN_COLUMN_DESTINATION_ID)) {
			importError = new ImportError(ImportError.ImportErrorType.RELATIONSHIP_DESTINATION_CONCEPT_DOES_NOT_EXIST, getConceptIdFromEndOfLine(message));
		} else if (message.startsWith(RELATIONSHIP_REFERS_TO_A_NON_EXISTING_CONCEPT_IN_COLUMN_SOURCE_ID)) {
			importError = new ImportError(ImportError.ImportErrorType.RELATIONSHIP_SOURCE_CONCEPT_DOES_NOT_EXIST, getConceptIdFromEndOfLine(message));
		} else if (message.startsWith(THE_CONCEPTS_BELOW_ARE_REFERENCED_IN_ACTIVE_RELATIONSHIPS)) {
			int conceptIdStartIndex = message.indexOf(SOURCE_CONCEPT) + SOURCE_CONCEPT.length();
			int conceptIdEndIndexQuote = message.indexOf("'", conceptIdStartIndex);
			int conceptIdEndIndexPipe = message.indexOf("|", conceptIdStartIndex);
			int conceptIdEndIndex = conceptIdEndIndexPipe != -1 && conceptIdEndIndexPipe < conceptIdEndIndexQuote ? conceptIdEndIndexPipe : conceptIdEndIndexQuote;
			importError = new ImportError(ImportError.ImportErrorType.CONCEPT_INACTIVATION_WHEN_REFERENCED_IN_ACTIVE_RELATIONSHIPS, message.substring(conceptIdStartIndex, conceptIdEndIndex));
		} else {
			logger.warn("Did not recognise import error message [{}]", message);
			importError = null;
		}
		return importError;
	}

	private String getConceptIdFromEndOfLine(String message) {
		return message.substring(message.indexOf(CONCEPT_ID) + CONCEPT_ID.length());
	}

	private long copyLogAndGetImportStartOffset(InputStream snowOwlRolloverLogStream, InputStream snowOwlLogStream, Path tempFile) throws ImportErrorParserException {
		Counter importStartOffset = new Counter(-1);
		try {
			Counter offset = new Counter(0);
			try (BufferedWriter writer = Files.newBufferedWriter(tempFile, Charset.forName(UTF_8))) {
				if (snowOwlRolloverLogStream != null) {
					logger.info("Copying snowowl rollover log for parsing.");
					getImportStartOffset(snowOwlRolloverLogStream, importStartOffset, offset, writer);
				}
				logger.info("Copying snowowl log for parsing.");
				getImportStartOffset(snowOwlLogStream, importStartOffset, offset, writer);
			}
		} catch (IOException e) {
			throw new ImportErrorParserException("IO error reading SnowOwl log or writing temp file.", e);
		}
		return importStartOffset.getCount();
	}

	private void getImportStartOffset(InputStream snowOwlLogStream, Counter importStartOffset, Counter offset, BufferedWriter writer) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(snowOwlLogStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(IMPORT_STARTED_FROM_RF2_RELEASE_FORMAT)) {
					importStartOffset.setCount(offset.getCount());
				}
				writer.write(line);
				writer.write(System.lineSeparator());
				offset.inc();
			}
		}
	}

	private void discardLinesBeforeImportStart(BufferedReader reader, long importStartOffset) throws IOException, ImportErrorParserException {
		for (long i = 0; i < importStartOffset; i++) {
			if (reader.readLine() == null) {
				throw new ImportErrorParserException("Log stream ran out before import offset reached.");
			}
		}
	}

	private static final class Counter {

		private long count;

		public Counter(int count) {
			this.count = count;
		}

		public void inc() {
			count++;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}
	}

}
