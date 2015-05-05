package org.ihtsdo.orchestration.clients.srs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.common.io.Files;

public class SRSRestClientHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSRestClientHelper.class);

	private static final String FILE_TYPE_INSERT = "****";
	private static final String RELEASE_DATE_INSERT = "########";

	public static final String UNKNOWN_EFFECTIVE_DATE = "Unpublished";
	public static final int EFFECTIVE_DATE_COLUMN = 1;
	public static final int CHARACTERISTIC_TYPE_ID_COLUMN = 8;
	public static final int TYPE_ID_COLUMN = 6;

	public static final String STATED_RELATIONSHIP_SCTID = "900000000000010007";
	public static final String TEXT_DEFINITION_SCTID = "900000000000550004";

	static Map<String, RefsetCombiner> refsetMap;
	static {
		refsetMap = new HashMap<String, RefsetCombiner>();
		refsetMap.put("Simple", new RefsetCombiner("der2_Refset_Simple****_INT_########.txt", new String[] {
				"der2_Refset_NonHumanSimpleReferenceSet****_INT_########.txt",
				"der2_Refset_VirtualMedicinalProductSimpleReferenceSet****_INT_########.txt",
				"der2_Refset_VirtualTherapeuticMoietySimpleReferenceSet****_INT_########.txt", }));

		refsetMap.put("AssociationReference", new RefsetCombiner("der2_cRefset_AssociationReference****_INT_########.txt", new String[] {
				"der2_cRefset_AlternativeAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_MovedFromAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_MovedToAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_PossiblyEquivalentToAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_RefersToConceptAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_ReplacedByAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_SameAsAssociationReferenceSet****_INT_########.txt",
				"der2_cRefset_WasAAssociationReferenceSet****_INT_########.txt", }));

		refsetMap.put("AttributeValue", new RefsetCombiner("der2_cRefset_AttributeValue****_INT_########.txt", new String[] {
				"der2_cRefset_ConceptInactivationIndicatorReferenceSet****_INT_########.txt",
				"der2_cRefset_DescriptionInactivationIndicatorReferenceSet****_INT_########.txt", }));

		refsetMap.put("Language", new RefsetCombiner("der2_cRefset_Language****-en_INT_########.txt", new String[] {
				"der2_cRefset_GbEnglish****-en-gb_INT_########.txt", "der2_cRefset_UsEnglish****-en-us_INT_########.txt" }));

		refsetMap.put("RefsetDescriptor", new RefsetCombiner("der2_cciRefset_RefsetDescriptor****_INT_########.txt", new String[] {

		}));
		refsetMap.put("DescriptionType", new RefsetCombiner("der2_ciRefset_DescriptionType****_INT_########.txt",
				new String[] { "der2_ciRefset_DescriptionFormat****_INT_########.txt" }));
		refsetMap.put("ComplexMap", new RefsetCombiner("der2_iissscRefset_ComplexMap****_INT_########.txt",
				new String[] { "der2_iissscRefset_Icd9CmEquivalenceComplexMapReferenceSet****_INT_########.txt" }));
		refsetMap.put("ExtendedMap", new RefsetCombiner("der2_iisssccRefset_ExtendedMap****_INT_########.txt",
				new String[] { "der2_iisssccRefset_Icd10ComplexMapReferenceSet****_INT_########.txt" }));
		refsetMap.put("SimpleMap", new RefsetCombiner("der2_sRefset_SimpleMap****_INT_########.txt", new String[] {
				"der2_sRefset_Ctv3SimpleMap****_INT_########.txt", "der2_sRefset_IcdOSimpleMapReferenceSet****_INT_########.txt",
				"der2_sRefset_SnomedRtIdSimpleMap****_INT_########.txt", "der2_sRefset_GmdnSimpleMapReferenceSet****_INT_########.txt" }));
		refsetMap.put("ModuleDependency", new RefsetCombiner("der2_ssRefset_ModuleDependency****_INT_########.txt",
				new String[] { "der2_ssRefset_ModuleDependency****_INT_########.txt" }));
	}

	/*
	 * @return - the directory containing the files ready for uploading to SRS
	 */
	public static File readyInputFiles(File archive, String releaseDate) throws ProcessWorkflowException, IOException {

		// We're going to create release files in a temp directory
		File extractDir = Files.createTempDir();
		unzipFlat(archive, extractDir);
		LOGGER.debug("Unzipped files to {}", extractDir.getAbsolutePath());

		// Merge the refsets into the expected files and replace any "unpublished" dates
		// with today's date
		mergeRefsets(extractDir, "Delta", releaseDate);
		replaceInFiles(extractDir, UNKNOWN_EFFECTIVE_DATE, releaseDate, EFFECTIVE_DATE_COLUMN);

		// The description file is currently named sct2_Description_${extractType}-en-gb_INT_<date>.txt
		// and we need it to be sct2_Description_${extractType}-en_INT_<date>.txt
		File descriptionFileWrongName = new File(extractDir, "sct2_Description_Delta-en-gb_INT_" + releaseDate + ".txt");
		File descriptionFileRightName = new File(extractDir, "sct2_Description_Delta-en_INT_" + releaseDate + ".txt");
		if (descriptionFileWrongName.exists()) {
			descriptionFileWrongName.renameTo(descriptionFileRightName);
		} else {
			LOGGER.warn("Was not able to find {} to correct the name", descriptionFileWrongName);
		}

		// We don't get a Stated Relationship file. We'll form it instead as a subset of the Inferred RelationshipFile
		File inferred = new File(extractDir, "sct2_Relationship_Delta_INT_" + releaseDate + ".txt");
		File stated = new File(extractDir, "sct2_StatedRelationship_Delta_INT_" + releaseDate + ".txt");
		boolean removeFromOriginal = false;
		boolean removeId = true;
		createSubsetFile(inferred, stated, CHARACTERISTIC_TYPE_ID_COLUMN, STATED_RELATIONSHIP_SCTID, removeFromOriginal, removeId);

		// We don't have a Text Definition file, so create that by extracting rows with TypeId 900000000000550004
		// from sct2_Description_Delta-en_INT_<date>.txt to form sct2_TextDefinition_Delta-en_INT_<date>.txt
		File description = new File(extractDir, "sct2_Description_Delta-en_INT_" + releaseDate + ".txt");
		File definition = new File(extractDir, "sct2_TextDefinition_Delta-en_INT_" + releaseDate + ".txt");
		removeFromOriginal = true;
		removeId = false;
		createSubsetFile(description, definition, TYPE_ID_COLUMN, TEXT_DEFINITION_SCTID, removeFromOriginal, removeId);

		// Now rename files to make the import compatible
		renameFiles(extractDir, "sct2", "rel2");
		renameFiles(extractDir, "der2", "rel2");

		return extractDir;
	}

	private static void mergeRefsets(File extractDir, String fileType, String releaseDate) throws IOException {
		// Loop through our map of refsets required, and see what contributing files we can match
		for (Map.Entry<String, RefsetCombiner> refset : refsetMap.entrySet()) {

			RefsetCombiner rc = (RefsetCombiner) refset.getValue();
			String combinedRefset = getFilename(rc.targetFilePattern, fileType, releaseDate);
			// Now can we find any of the contributing files to add to that file?
			boolean isFirstContributor = true;
			for (String contributorPattern : rc.sourceFilePatterns) {
				String contributorFilename = getFilename(contributorPattern, fileType, releaseDate);
				File contributorFile = new File(extractDir, contributorFilename);
				File combinedRefsetFile = new File(extractDir, combinedRefset);
				if (contributorFile.exists()) {
					List<String> fileLines = FileUtils.readLines(contributorFile, StandardCharsets.UTF_8);
					// Don't need the header line for any subsequent files
					if (!isFirstContributor) {
						fileLines.remove(0);
					}
					boolean append = !isFirstContributor;
					FileUtils.writeLines(combinedRefsetFile, fileLines, append);
					isFirstContributor = false;
					// Now we can delete the contributor so it doesn't get uploaded as another input file
					contributorFile.delete();
				}
			}
			if (isFirstContributor) {
				LOGGER.warn("Failed to find any files to contribute to {}", combinedRefset);
			} else {
				LOGGER.debug("Created combined refset {}", combinedRefset);
			}
		}
	}

	private static String getFilename(String filenamePattern, String fileType, String date) {
		return filenamePattern.replace(FILE_TYPE_INSERT, fileType).replace(RELEASE_DATE_INSERT, date);
	}

	private static void renameFiles(File targetDirectory, String find, String replace) {
		Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
				+ " must be a directory in order to rename files from " + find + " to " + replace);
		for (File thisFile : targetDirectory.listFiles()) {
			if (thisFile.exists() && !thisFile.isDirectory()) {
				String currentName = thisFile.getName();
				String newName = currentName.replace(find, replace);
				if (!newName.equals(currentName)) {
					File newFile = new File(targetDirectory, newName);
					thisFile.renameTo(newFile);
				}
			}
		}
	}

	/**
	 * @param targetDirectory
	 * @param find
	 * @param replace
	 * @param columnNum
	 *            searched for term must match in this column
	 * @throws IOException
	 */
	protected static void replaceInFiles(File targetDirectory, String find, String replace, int columnNum) throws IOException {
		Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
				+ " must be a directory in order to replace text from " + find + " to " + replace);
		for (File thisFile : targetDirectory.listFiles()) {
			if (thisFile.exists() && !thisFile.isDirectory()) {
				List<String> oldLines = FileUtils.readLines(thisFile, StandardCharsets.UTF_8);
				List<String> newLines = new ArrayList<String>();
				for (String thisLine : oldLines) {
					String[] columns = thisLine.split("\t");
					if (columns.length > columnNum & columns[columnNum].equals(find)) {
						thisLine = thisLine.replaceFirst(find, replace); // Would be more generic to rebuild from columns
					}
					newLines.add(thisLine);
				}
				FileUtils.writeLines(thisFile, newLines);
			}
		}
	}

	/*
	 * Creates a file containing all the rows which have "mustMatch" in columnNum. Plus the header row.
	 */
	protected static void createSubsetFile(File source, File target, int columnNum, String mustMatch, boolean removeFromOriginal,
			boolean removeId)
			throws IOException {
		if (source.exists() && !source.isDirectory()) {
			LOGGER.debug("Creating {} as a subset of {} and {} rows in original.", target, source, (removeFromOriginal ? "removing"
					: "leaving"));
			List<String> allLines = FileUtils.readLines(source, StandardCharsets.UTF_8);
			List<String> newLines = new ArrayList<String>();
			List<String> remainingLines = new ArrayList<String>();
			int lineCount = 1;
			for (String thisLine : allLines) {
				String[] columns = thisLine.split("\t");
				if (lineCount == 1 || (columns.length > columnNum && columns[columnNum].equals(mustMatch))) {
					// Are we wiping out the Id (column index 0) before writing?
					if (removeId && lineCount != 1) {
						columns[0] = "";
						String lineWithIDRemoved = StringUtils.join(columns, "\t");
						newLines.add(lineWithIDRemoved);
					} else {
						newLines.add(thisLine);
					}
					if (lineCount == 1) {
						remainingLines.add(thisLine);
					}
				} else {
					remainingLines.add(thisLine);
				}
				lineCount++;
			}
			FileUtils.writeLines(target, newLines);
			if (removeFromOriginal) {
				FileUtils.writeLines(source, remainingLines);
			}
		} else {
			LOGGER.warn("Did not find file {} needed to create subset {}", source, target);
		}
	}

	public static String recoverReleaseDate(File archive) throws ProcessWorkflowException, IOException {
		// Ensure that we have a valid archive
		if (!archive.isFile()) {
			throw new ProcessWorkflowException("Could not open supplied archive: " + archive.getAbsolutePath());
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry ze = zis.getNextEntry();
		try {
			while (ze != null) {
				if (!ze.isDirectory()) {
					return findDateInString(ze.getName());
				}
				ze = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
		}
		throw new ProcessWorkflowException("No files found in archive: " + archive.getAbsolutePath());
	}

	public static String findDateInString(String str) throws ProcessWorkflowException {
		Matcher dateMatcher = Pattern.compile("(\\d{8})").matcher(str);
		if (dateMatcher.find()) {
			return dateMatcher.group();
		}
		throw new ProcessWorkflowException("Unable to determine date from " + str);
	}

	public static void unzipFlat(File archive, File targetDir) throws ProcessWorkflowException, IOException {

		if (!targetDir.exists() || !targetDir.isDirectory()) {
			throw new ProcessWorkflowException(targetDir + " is not a viable directory in which to extract archive");
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry ze = zis.getNextEntry();
		try {
			while (ze != null) {
				if (!ze.isDirectory()) {
					Path p = Paths.get(ze.getName());
					String extractedFileName = p.getFileName().toString();
					File extractedFile = new File(targetDir, extractedFileName);
					OutputStream out = new FileOutputStream(extractedFile);
					IOUtils.copy(zis, out);
					IOUtils.closeQuietly(out);
				}
				ze = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
		}
	}

}
