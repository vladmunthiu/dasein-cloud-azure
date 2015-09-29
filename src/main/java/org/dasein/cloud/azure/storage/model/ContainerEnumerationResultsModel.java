package org.dasein.cloud.azure.storage.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="EnumerationResults")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainerEnumerationResultsModel extends BaseEnumerationResultsModel {

	@XmlAttribute(name="ServiceEndpoint")
	private String serviceEndpoint;
	
	@XmlElementWrapper(name="Containers")
	@XmlElement(name="Container")
	private List<ContainerModel> containers;

	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}
	
	public List<ContainerModel> getContainers() {
		return containers;
	}

	public void setContainers(List<ContainerModel> containers) {
		this.containers = containers;
	}
	
}
