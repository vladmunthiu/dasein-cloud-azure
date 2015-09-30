package org.dasein.cloud.azure.compute.disk.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Disks", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisksModel {

	@XmlElement(name="Disk", namespace ="http://schemas.microsoft.com/windowsazure")
	private List<DiskModel> disks;

	public List<DiskModel> getDisks() {
		return disks;
	}

	public void setDisks(List<DiskModel> disks) {
		this.disks = disks;
	}
	
}
