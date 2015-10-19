package org.dasein.cloud.azure.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class Location {

	@XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
	private String name;
	@XmlElement(name="DisplayName", namespace ="http://schemas.microsoft.com/windowsazure")
	private String displayName;
	@XmlElementWrapper(name="AvailableServices", namespace ="http://schemas.microsoft.com/windowsazure")
	@XmlElement(name="AvailableService", namespace ="http://schemas.microsoft.com/windowsazure")
	private List<String> availableServices;
	@XmlElement(name="ComputeCapabilities", namespace ="http://schemas.microsoft.com/windowsazure")
	private ComputeCapabilities computeCapabilities;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public List<String> getAvailableServices() {
		return availableServices;
	}
	public void setAvailableServices(List<String> availableServices) {
		this.availableServices = availableServices;
	}
	public ComputeCapabilities getComputeCapabilities() {
		return computeCapabilities;
	}
	public void setComputeCapabilities(ComputeCapabilities computeCapabilities) {
		this.computeCapabilities = computeCapabilities;
	}
}
