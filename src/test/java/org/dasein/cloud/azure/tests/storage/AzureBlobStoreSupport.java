package org.dasein.cloud.azure.tests.storage;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.storage.BlobStore;
import org.dasein.cloud.storage.Blob;

public class AzureBlobStoreSupport extends BlobStore {

	private boolean isBucketNameExists;
	
	public AzureBlobStoreSupport(Azure provider) {
		this(provider, false);
	}
	
	public AzureBlobStoreSupport(Azure provider, boolean isBucketNameExists) {
		super(provider);
		this.isBucketNameExists = isBucketNameExists;
	}
	
	@Override
	public boolean exists(@Nonnull String bucketName) throws InternalException, CloudException {
		return isBucketNameExists;
	}

}
