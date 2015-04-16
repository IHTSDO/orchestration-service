package org.ihtsdo.orchestration.clients.srs;

public class RefsetCombiner {

	String targetFilePattern;
	String[] sourceFilePatterns;

	public RefsetCombiner(String targetFilePattern, String[] sourceFilePatterns) {
		this.targetFilePattern = targetFilePattern;
		this.sourceFilePatterns = sourceFilePatterns;
	}
}
