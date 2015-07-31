package org.ihtsdo.orchestration.clients.srs;

import java.io.File;

import org.ihtsdo.otf.utils.DateUtils;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class SRSProjectConfiguration {

	File inputFilesDir;
	String releaseDate;
	String productName;
	String readmeEndDate;

	boolean firstTimeRelease = false;
	boolean workbenchDataFixesRequired = false;
	boolean inputFileFixesRequired = true;
	boolean createInferredRelationships = true;
	boolean justPackage = false;
	boolean createLegacyIds = true;
	boolean beta = false;
	String previousPublishedPackageName;

	// These parameters are set in the SRS but passed on to the RVF
	String previousInternationalRelease;
	String assertionGroupNames;

	// Apache BeanUtils.copyProperties could also be used. Less code but slower
	public SRSProjectConfiguration clone() {
		SRSProjectConfiguration clone = new SRSProjectConfiguration();
		clone.inputFilesDir = this.inputFilesDir;
		clone.releaseDate = this.releaseDate;
		clone.productName = this.productName;
		clone.readmeEndDate = this.readmeEndDate;
		clone.firstTimeRelease = this.firstTimeRelease;
		clone.workbenchDataFixesRequired = this.workbenchDataFixesRequired;
		clone.inputFileFixesRequired = this.inputFileFixesRequired;
		clone.createInferredRelationships = this.createInferredRelationships;
		clone.justPackage = this.justPackage;
		clone.createLegacyIds = this.createLegacyIds;
		clone.beta = this.beta;
		clone.previousPublishedPackageName = this.previousPublishedPackageName;
		clone.previousInternationalRelease = this.previousInternationalRelease;
		clone.assertionGroupNames = this.assertionGroupNames;
		return clone;
	}

	public File getInputFilesDir() {
		return inputFilesDir;
	}

	public void setInputFilesDir(File archiveLocation) {
		this.inputFilesDir = archiveLocation;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * The script client usually sets these parameters: 
	 * effectiveDate="2015-08-31" 
	 * readmeEndDate="2015" 
	 * isFirstTime=false
	 * isWorkbenchDataFixesRequired=false 
	 * isInputFilesFixesRequired=true 
	 * createInferredRelationships=true 
	 * createLegacyIds=true
	 * previousPublishedPackageName="SnomedCT_RF2Release_INT_20150731.zip" 
	 * previousInternationalRelease="20150731"
	 * assertionGroupNames="component-centric-validation,
	 * file-centric-validation,release-type-validation"
	 * productName="StatedSubset_TechPreview" 
	 * justPackage=false
	 * 
	 * @throws JSONException
	 **/
	public JSONObject getJson() throws JSONException {
		JSONObject jsonObj = new JSONObject();

		String releaseDateISO = DateUtils.formatAsISO(getReleaseDate());
		jsonObj.put("effectiveTime", releaseDateISO);
		jsonObj.put("inputFilesFixesRequired", Boolean.toString(inputFileFixesRequired));
		jsonObj.put("firstTimeRelease", Boolean.toString(firstTimeRelease));
		jsonObj.put("workbenchDataFixesRequired", Boolean.toString(workbenchDataFixesRequired));
		jsonObj.put("createInferredRelationships", Boolean.toString(createInferredRelationships));
		jsonObj.put("justPackage", Boolean.toString(justPackage));
		jsonObj.put("createLegacyIds", Boolean.toString(createLegacyIds));
		jsonObj.put("previousPublishedPackage", previousPublishedPackageName);

		// These parameters passed through to RVF
		jsonObj.put("previousInternationalRelease", previousInternationalRelease);
		jsonObj.put("assertionGroupNames", assertionGroupNames);
		return jsonObj;
	}

	public String getReadmeEndDate() {
		return readmeEndDate;
	}

	public void setReadmeEndDate(String readmeEndDate) {
		this.readmeEndDate = readmeEndDate;
	}

	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}

	public void setFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
	}

	public boolean isWorkbenchDataFixesRequired() {
		return workbenchDataFixesRequired;
	}

	public void setWorkbenchDataFixesRequired(boolean workbenchDataFixesRequired) {
		this.workbenchDataFixesRequired = workbenchDataFixesRequired;
	}

	public boolean isInputFileFixesRequired() {
		return inputFileFixesRequired;
	}

	public void setInputFileFixesRequired(boolean inputFileFixesRequired) {
		this.inputFileFixesRequired = inputFileFixesRequired;
	}

	public boolean isCreateInferredRelationships() {
		return createInferredRelationships;
	}

	public void setCreateInferredRelationships(boolean createInferredRelationships) {
		this.createInferredRelationships = createInferredRelationships;
	}

	public boolean isJustPackage() {
		return justPackage;
	}

	public void setJustPackage(boolean justPackage) {
		this.justPackage = justPackage;
	}

	public boolean isCreateLegacyIds() {
		return createLegacyIds;
	}

	public void setCreateLegacyIds(boolean createLegacyIds) {
		this.createLegacyIds = createLegacyIds;
	}

	public boolean isBeta() {
		return beta;
	}

	public void setBeta(boolean beta) {
		this.beta = beta;
	}

	public String getPreviousPublishedPackageName() {
		return previousPublishedPackageName;
	}

	public void setPreviousPublishedPackageName(String previousPublishedPackageName) {
		this.previousPublishedPackageName = previousPublishedPackageName;
	}

	public String getPreviousInternationalRelease() {
		return previousInternationalRelease;
	}

	public void setPreviousInternationalRelease(String previousInternationalRelease) {
		this.previousInternationalRelease = previousInternationalRelease;
	}

	public String getAssertionGroupNames() {
		return assertionGroupNames;
	}

	public void setAssertionGroupNames(String assertionGroupNames) {
		this.assertionGroupNames = assertionGroupNames;
	}
}
