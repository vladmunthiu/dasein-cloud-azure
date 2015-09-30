package org.dasein.cloud.azure.storage.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class BasePropertiesModel {

	@XmlElement(name="Last-Modified")
	private String lastModified;
	@XmlElement(name="Etag")
	private String eTag;
	@XmlElement(name="LeaseStatus")
	private String leaseStatus;
	@XmlElement(name="LeaseState")
	private String leaseState;
	@XmlElement(name="LeaseDuration")
	private String leaseDuration;
	
	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public String getLeaseStatus() {
		return leaseStatus;
	}
	public void setLeaseStatus(String leaseStatus) {
		this.leaseStatus = leaseStatus;
	}
	public String getLeaseState() {
		return leaseState;
	}
	public void setLeaseState(String leaseState) {
		this.leaseState = leaseState;
	}
	public String getLeaseDuration() {
		return leaseDuration;
	}
	public void setLeaseDuration(String leaseDuration) {
		this.leaseDuration = leaseDuration;
	}
}
