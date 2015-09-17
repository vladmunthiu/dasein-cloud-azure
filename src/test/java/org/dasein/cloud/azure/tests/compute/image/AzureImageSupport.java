package org.dasein.cloud.azure.tests.compute.image;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.compute.MachineImage;

public class AzureImageSupport extends AzureOSImage {

	private MachineImage image;
	
	public AzureImageSupport(Azure provider) {
		this(provider, null);
	}

	public AzureImageSupport(Azure provider, MachineImage image) {
		super(provider);
		this.image = image;
	}

	@Override public MachineImage getImage(String id) {
		return image;
	}
	
}
