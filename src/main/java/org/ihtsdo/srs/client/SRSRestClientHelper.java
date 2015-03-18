package org.ihtsdo.srs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class SRSRestClientHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSRestClientHelper.class);

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
				"der2_sRefset_SnomedRtIdSimpleMap****_INT_########.txt" }));
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

		return extractDir;
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
