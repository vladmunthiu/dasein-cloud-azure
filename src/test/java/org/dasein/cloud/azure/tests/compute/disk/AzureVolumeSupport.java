package org.dasein.cloud.azure.tests.compute.disk;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.compute.disk.AzureDisk;
import org.dasein.cloud.compute.Volume;

public class AzureVolumeSupport extends AzureDisk {

	private Volume volume;
	
	public AzureVolumeSupport(Azure provider) {
		this(provider, null);
	}
	
	public AzureVolumeSupport(Azure provider, Volume volume) {
		super(provider);
		this.volume = volume;
	}
	
	@Override public Volume getVolume(String volumeId) {
		return volume;
	}
	
}
