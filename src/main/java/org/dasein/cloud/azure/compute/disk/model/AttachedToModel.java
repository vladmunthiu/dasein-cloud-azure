package org.dasein.cloud.azure.compute.disk.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="AttachedTo", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class AttachedToModel {

	@XmlElement(name="HostedServiceName", namespace="http://schemas.microsoft.com/windowsazure")
	private String hostedServiceName;
	@XmlElement(name="DeploymentName", namespace="http://schemas.microsoft.com/windowsazure")
	private String deploymentName;
	@XmlElement(name="RoleName", namespace="http://schemas.microsoft.com/windowsazure")
	private String roleName;
	
	public String getHostedServiceName() {
		return hostedServiceName;
	}
	public void setHostedServiceName(String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}
	public String getDeploymentName() {
		return deploymentName;
	}
	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	
}
