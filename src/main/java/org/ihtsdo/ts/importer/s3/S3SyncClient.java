package org.ihtsdo.ts.importer.s3;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class S3SyncClient {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String bucketName;


	public List<String> listFiles() {
		ObjectListing objectListing = s3Client.listObjects(bucketName, "");
		List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
		List<String> filenames = new ArrayList<String>();
		for (S3ObjectSummary objectSummary : objectSummaries) {
			filenames.add(objectSummary.getKey());
		}

		return filenames;
	}
}
