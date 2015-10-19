package org.dasein.cloud.azure.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ComputeCapabilities", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class ComputeCapabilities {

	@XmlElementWrapper(name="VirtualMachineRoleSizes", namespace ="http://schemas.microsoft.com/windowsazure")
	@XmlElement(name="RoleSize", namespace ="http://schemas.microsoft.com/windowsazure")
	private List<String> virtualMachineRoleSizeNames;
	@XmlElementWrapper(name="WebWorkerRoleSizes", namespace ="http://schemas.microsoft.com/windowsazure")
	@XmlElement(name="RoleSize", namespace ="http://schemas.microsoft.com/windowsazure")
	private List<String> webWorkerRoleSizeNames;
	
	public List<String> getVirtualMachineRoleSizeNames() {
		return virtualMachineRoleSizeNames;
	}
	public void setVirtualMachineRoleSizeNames(
			List<String> virtualMachineRoleSizeNames) {
		this.virtualMachineRoleSizeNames = virtualMachineRoleSizeNames;
	}
	public List<String> getWebWorkerRoleSizeNames() {
		return webWorkerRoleSizeNames;
	}
	public void setWebWorkerRoleSizeNames(List<String> webWorkerRoleSizeNames) {
		this.webWorkerRoleSizeNames = webWorkerRoleSizeNames;
	}
}
