package org.dasein.cloud.azure.compute.disk.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Disk", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DiskModel {

	@XmlElement(name="AffinityGroup", namespace="http://schemas.microsoft.com/windowsazure")
	private String affinityGroup;
	@XmlElement(name="OS", namespace="http://schemas.microsoft.com/windowsazure")
	private String os;
	@XmlElement(name="Location", namespace="http://schemas.microsoft.com/windowsazure")
	private String location;
	@XmlElement(name="LogicalDiskSizeInGB", namespace="http://schemas.microsoft.com/windowsazure")
	private String logicalDiskSizeInGB;
	@XmlElement(name="MediaLink", namespace="http://schemas.microsoft.com/windowsazure")
	private String mediaLink;
	@XmlElement(name="Name", namespace="http://schemas.microsoft.com/windowsazure")
	private String name;
	@XmlElement(name="SourceImageName", namespace="http://schemas.microsoft.com/windowsazure")
	private String sourceImageName;
	@XmlElement(name="CreatedTime", namespace="http://schemas.microsoft.com/windowsazure")
	private String createdTime;
	@XmlElement(name="IOType", namespace="http://schemas.microsoft.com/windowsazure")
	private String ioType;
	@XmlElement(name="AttachedTo", namespace="http://schemas.microsoft.com/windowsazure")
	private AttachedToModel attachedTo;
	
	public String getAffinityGroup() {
		return affinityGroup;
	}
	public void setAffinityGroup(String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getLogicalDiskSizeInGB() {
		return logicalDiskSizeInGB;
	}
	public void setLogicalDiskSizeInGB(String logicalDiskSizeInGB) {
		this.logicalDiskSizeInGB = logicalDiskSizeInGB;
	}
	public String getMediaLink() {
		return mediaLink;
	}
	public void setMediaLink(String mediaLink) {
		this.mediaLink = mediaLink;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSourceImageName() {
		return sourceImageName;
	}
	public void setSourceImageName(String sourceImageName) {
		this.sourceImageName = sourceImageName;
	}
	public String getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}
	public String getIoType() {
		return ioType;
	}
	public void setIoType(String ioType) {
		this.ioType = ioType;
	}
	public AttachedToModel getAttachedTo() {
		return attachedTo;
	}
	public void setAttachedTo(AttachedToModel attachedTo) {
		this.attachedTo = attachedTo;
	}
	
}
