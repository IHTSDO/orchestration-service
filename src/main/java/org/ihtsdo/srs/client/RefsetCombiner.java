package org.ihtsdo.srs.client;

public class RefsetCombiner {

	String targetFilePattern;
	String[] sourceFilePatterns;

	public RefsetCombiner(String targetFilePattern, String[] sourceFilePatterns) {
		this.targetFilePattern = targetFilePattern;
		this.sourceFilePatterns = sourceFilePatterns;
	}
}
