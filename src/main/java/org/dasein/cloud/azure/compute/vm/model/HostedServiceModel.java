package org.dasein.cloud.azure.compute.vm.model;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Created by vmunthiu on 9/22/2015.
 */
@XmlRootElement(name="HostedService", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class HostedServiceModel {
    @XmlElement(name="Url", namespace ="http://schemas.microsoft.com/windowsazure")
    private String url;
    @XmlElement(name="ServiceName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serviceName;
    @XmlElement(name="HostedServiceProperties", namespace ="http://schemas.microsoft.com/windowsazure")
    private HostedServiceProperties hostedServiceProperties;

    @XmlElementWrapper(name = "Deployments")
    @XmlElement(name="Deployment")
    private List<Deployment> deployments;
    @XmlElement(name="DefaultWinRmCertificateThumbprint", namespace ="http://schemas.microsoft.com/windowsazure")
    private String defaultWinRmCertificateThumbprint;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public HostedServiceProperties getHostedServiceProperties() {
        return hostedServiceProperties;
    }

    public void setHostedServiceProperties(HostedServiceProperties hostedServiceProperties) {
        this.hostedServiceProperties = hostedServiceProperties;
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<Deployment> deployments) {
        this.deployments = deployments;
    }

    public String getDefaultWinRmCertificateThumbprint() {
        return defaultWinRmCertificateThumbprint;
    }

    public void setDefaultWinRmCertificateThumbprint(String defaultWinRmCertificateThumbprint) {
        this.defaultWinRmCertificateThumbprint = defaultWinRmCertificateThumbprint;
    }

    @XmlRootElement(name="Deployment")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Deployment {
        @XmlElement(name="Name")
        private String name;
        @XmlElement(name="DeploymentSlot")
        private String deploymentSlot;
        @XmlElement(name="PrivateID")
        private String privateId;
        @XmlElement(name="Status")
        private String status;
        @XmlElement(name="Label")
        private String label;
        @XmlElement(name="Url")
        private String url;
        @XmlElement(name="Configuration")
        private String configuration;
        @XmlElementWrapper(name = "RoleInstanceList")
        @XmlElement(name="RoleInstance")
        private List<RoleInstance> roleInstanceList;
        @XmlElement(name="UpgradeStatus", namespace ="http://schemas.microsoft.com/windowsazure")
        private UpgradeStatus upgradeStatus;
        @XmlElement(name="UpgradeDomainCount", namespace ="http://schemas.microsoft.com/windowsazure")
        private String upgradeDomainCount;
        @XmlElementWrapper(name = "RoleList")
        @XmlElement(name="Role")
        private List<Role> roleList;
        @XmlElement(name="SdkVersion")
        private String sdkVersion;
        @XmlElement(name="Locked")
        private String locked;
        @XmlElement(name="RollbackAllowed")
        private String rollbackAllowed;
        @XmlElement(name="CreatedTime")
        private String createdTime;
        @XmlElement(name="LastModifiedTime")
        private String lastModifiedTime;
        @XmlElement(name="VirtualNetworkName")
        private String virtualNetworkName;
        @XmlElement(name="Dns")
        private Dns dns;
        @XmlElementWrapper(name = "ExtendedProperties")
        @XmlElement(name="ExtendedProperty")
        private List<ExtendedProperty> extendedProperties;
        @XmlElement(name="PersistentVMDowntime")
        private PersistentVMDowntime persistentVMDowntime;
        @XmlElementWrapper(name = "VirtualIPs")
        @XmlElement(name="VirtualIP")
        private List<VirtualIP> virtualIPs;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDeploymentSlot() {
            return deploymentSlot;
        }

        public void setDeploymentSlot(String deploymentSlot) {
            this.deploymentSlot = deploymentSlot;
        }

        public String getPrivateId() {
            return privateId;
        }

        public void setPrivateId(String privateId) {
            this.privateId = privateId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getConfiguration() {
            return configuration;
        }

        public void setConfiguration(String configuration) {
            this.configuration = configuration;
        }

        public List<RoleInstance> getRoleInstanceList() {
            return roleInstanceList;
        }

        public void setRoleInstanceList(List<RoleInstance> roleInstanceList) {
            this.roleInstanceList = roleInstanceList;
        }

        public UpgradeStatus getUpgradeStatus() {
            return upgradeStatus;
        }

        public void setUpgradeStatus(UpgradeStatus upgradeStatus) {
            this.upgradeStatus = upgradeStatus;
        }

        public String getUpgradeDomainCount() {
            return upgradeDomainCount;
        }

        public void setUpgradeDomainCount(String upgradeDomainCount) {
            this.upgradeDomainCount = upgradeDomainCount;
        }

        public List<Role> getRoleList() {
            return roleList;
        }

        public void setRoleList(List<Role> roleList) {
            this.roleList = roleList;
        }

        public String getSdkVersion() {
            return sdkVersion;
        }

        public void setSdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
        }

        public String getLocked() {
            return locked;
        }

        public void setLocked(String locked) {
            this.locked = locked;
        }

        public String getRollbackAllowed() {
            return rollbackAllowed;
        }

        public void setRollbackAllowed(String rollbackAllowed) {
            this.rollbackAllowed = rollbackAllowed;
        }

        public String getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }

        public String getLastModifiedTime() {
            return lastModifiedTime;
        }

        public void setLastModifiedTime(String lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
        }

        public String getVirtualNetworkName() {
            return virtualNetworkName;
        }

        public void setVirtualNetworkName(String virtualNetworkName) {
            this.virtualNetworkName = virtualNetworkName;
        }

        public Dns getDns() {
            return dns;
        }

        public void setDns(Dns dns) {
            this.dns = dns;
        }

        public List<ExtendedProperty> getExtendedProperties() {
            return extendedProperties;
        }

        public void setExtendedProperties(List<ExtendedProperty> extendedProperties) {
            this.extendedProperties = extendedProperties;
        }

        public PersistentVMDowntime getPersistentVMDowntime() {
            return persistentVMDowntime;
        }

        public void setPersistentVMDowntime(PersistentVMDowntime persistentVMDowntime) {
            this.persistentVMDowntime = persistentVMDowntime;
        }

        public List<VirtualIP> getVirtualIPs() {
            return virtualIPs;
        }

        public void setVirtualIPs(List<VirtualIP> virtualIPs) {
            this.virtualIPs = virtualIPs;
        }
    }

    @XmlRootElement(name="VirtualIP")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VirtualIP {
        @XmlElement(name="Address")
        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    @XmlRootElement(name="PersistentVMDowntime")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PersistentVMDowntime {
        @XmlElement(name="StartTime")
        private String startTime;
        @XmlElement(name="EndTime")
        private String endTime;
        @XmlElement(name="Status")
        private String status;

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @XmlRootElement(name="Dns")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Dns {
        @XmlElementWrapper(name = "DnsServers")
        @XmlElement(name="DnsServer")
        private List<DnsServer> dnsServers;

        public List<DnsServer> getDnsServers() {
            return dnsServers;
        }

        public void setDnsServers(List<DnsServer> dnsServers) {
            this.dnsServers = dnsServers;
        }
    }

    @XmlRootElement(name="DnsServer")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DnsServer {
        @XmlElement(name="Name")
        private String name;
        @XmlElement(name="Address")
        private String address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    @XmlRootElement(name="Role")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Role {
        @XmlElement(name="RoleName")
        private String roleName;
        @XmlElement(name="OSVersion")
        private String osVersion;
        @XmlElement(name="RoleType")
        private String roleType;
        @XmlElementWrapper(name = "ConfigurationSets")
        @XmlElement(name="ConfigurationSet")
        private List<ConfigurationSet> configurationSets;
        @XmlElement(name="AvailabilitySetName")
        private String availabilitySetName;
        @XmlElementWrapper(name = "DataVirtualHardDisks")
        @XmlElement(name="DataVirtualHardDisk")
        private List<DataVirtualHardDisk> dataVirtualHardDisks;
        @XmlElement(name="OSVirtualHardDisk")
        private OSVirtualHardDisk osVirtualHardDisk;
        @XmlElement(name="RoleSize")
        private String roleSize;

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public List<ConfigurationSet> getConfigurationSets() {
            return configurationSets;
        }

        public void setConfigurationSets(List<ConfigurationSet> configurationSets) {
            this.configurationSets = configurationSets;
        }

        public String getAvailabilitySetName() {
            return availabilitySetName;
        }

        public void setAvailabilitySetName(String availabilitySetName) {
            this.availabilitySetName = availabilitySetName;
        }

        public List<DataVirtualHardDisk> getDataVirtualHardDisks() {
            return dataVirtualHardDisks;
        }

        public void setDataVirtualHardDisks(List<DataVirtualHardDisk> dataVirtualHardDisks) {
            this.dataVirtualHardDisks = dataVirtualHardDisks;
        }

        public OSVirtualHardDisk getOsVirtualHardDisk() {
            return osVirtualHardDisk;
        }

        public void setOsVirtualHardDisk(OSVirtualHardDisk osVirtualHardDisk) {
            this.osVirtualHardDisk = osVirtualHardDisk;
        }

        public String getRoleSize() {
            return roleSize;
        }

        public void setRoleSize(String roleSize) {
            this.roleSize = roleSize;
        }
    }

    @XmlRootElement(name="DataVirtualHardDisk")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DataVirtualHardDisk {
        @XmlElement(name="HostCaching")
        private String hostCaching;
        @XmlElement(name="DiskName")
        private String diskName;
        @XmlElement(name="Lun")
        private String lun;
        @XmlElement(name="LogicalDiskSizeInGB")
        private String logicalDiskSizeInGB;
        @XmlElement(name="MediaLink")
        private String mediaLink;

        public String getHostCaching() {
            return hostCaching;
        }

        public void setHostCaching(String hostCaching) {
            this.hostCaching = hostCaching;
        }

        public String getDiskName() {
            return diskName;
        }

        public void setDiskName(String diskName) {
            this.diskName = diskName;
        }

        public String getLun() {
            return lun;
        }

        public void setLun(String lun) {
            this.lun = lun;
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
    }

    @XmlRootElement(name="OSVirtualHardDisk")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OSVirtualHardDisk {
        @XmlElement(name="HostCaching")
        private String hostCaching;
        @XmlElement(name="DiskName")
        private String diskName;
        @XmlElement(name="MediaLink")
        private String mediaLink;
        @XmlElement(name="SourceImageName")
        private String sourceImageName;
        @XmlElement(name="OS")
        private String os;

        public String getHostCaching() {
            return hostCaching;
        }

        public void setHostCaching(String hostCaching) {
            this.hostCaching = hostCaching;
        }

        public String getDiskName() {
            return diskName;
        }

        public void setDiskName(String diskName) {
            this.diskName = diskName;
        }

        public String getMediaLink() {
            return mediaLink;
        }

        public void setMediaLink(String mediaLink) {
            this.mediaLink = mediaLink;
        }

        public String getSourceImageName() {
            return sourceImageName;
        }

        public void setSourceImageName(String sourceImageName) {
            this.sourceImageName = sourceImageName;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }
    }
    @XmlRootElement(name="ConfigurationSet")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ConfigurationSet {
        @XmlElement(name="ConfigurationSetType")
        private String configurationSetType;
        @XmlElementWrapper(name = "InputEndpoints")
        @XmlElement(name="InputEndpoint")
        private List<InputEndpoint> inputEndpoints;
        @XmlElementWrapper(name = "SubnetNames")
        @XmlElement(name="SubnetName")
        private List<String> subnetNames;

        public String getConfigurationSetType() {
            return configurationSetType;
        }

        public void setConfigurationSetType(String configurationSetType) {
            this.configurationSetType = configurationSetType;
        }

        public List<InputEndpoint> getInputEndpoints() {
            return inputEndpoints;
        }

        public void setInputEndpoints(List<InputEndpoint> inputEndpoints) {
            this.inputEndpoints = inputEndpoints;
        }

        public List<String> getSubnetNames() {
            return subnetNames;
        }

        public void setSubnetNames(List<String> subnetNames) {
            this.subnetNames = subnetNames;
        }
    }

    @XmlRootElement(name="InputEndpoint")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InputEndpoint {
        @XmlElement(name="LoadBalancedEndpointSetName")
        private String loadBalancedEndpointSetName;
        @XmlElement(name="LocalPort")
        private String localPort;
        @XmlElement(name="Name")
        private String name;
        @XmlElement(name="Port")
        private String port;
        @XmlElement(name="LoadBalancerProbe")
        private LoadBalancerProbe loadBalancerProbe;
        @XmlElement(name="Protocol")
        private String protocol;
        @XmlElement(name="Vip")
        private String vip;

        public String getLoadBalancedEndpointSetName() {
            return loadBalancedEndpointSetName;
        }

        public void setLoadBalancedEndpointSetName(String loadBalancedEndpointSetName) {
            this.loadBalancedEndpointSetName = loadBalancedEndpointSetName;
        }

        public String getLocalPort() {
            return localPort;
        }

        public void setLocalPort(String localPort) {
            this.localPort = localPort;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public LoadBalancerProbe getLoadBalancerProbe() {
            return loadBalancerProbe;
        }

        public void setLoadBalancerProbe(LoadBalancerProbe loadBalancerProbe) {
            this.loadBalancerProbe = loadBalancerProbe;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getVip() {
            return vip;
        }

        public void setVip(String vip) {
            this.vip = vip;
        }
    }

    @XmlRootElement(name="LoadBalancerProbe")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LoadBalancerProbe {
        @XmlElement(name="Path")
        private String path;
        @XmlElement(name="Port")
        private String port;
        @XmlElement(name="Protocol")
        private String protocol;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }

    @XmlRootElement(name="UpgradeStatus")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UpgradeStatus {
        @XmlElement(name="UpgradeType")
        private String upgradeType;
        @XmlElement(name="CurrentUpgradeDomainState")
        private String currentUpgradeDomainState;
        @XmlElement(name="CurrentUpgradeDomain")
        private String currentUpgradeDomain;

        public String getUpgradeType() {
            return upgradeType;
        }

        public void setUpgradeType(String upgradeType) {
            this.upgradeType = upgradeType;
        }

        public String getCurrentUpgradeDomainState() {
            return currentUpgradeDomainState;
        }

        public void setCurrentUpgradeDomainState(String currentUpgradeDomainState) {
            this.currentUpgradeDomainState = currentUpgradeDomainState;
        }

        public String getCurrentUpgradeDomain() {
            return currentUpgradeDomain;
        }

        public void setCurrentUpgradeDomain(String currentUpgradeDomain) {
            this.currentUpgradeDomain = currentUpgradeDomain;
        }
    }

    @XmlRootElement(name="RoleInstance")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoleInstance {
        @XmlElement(name="RoleName")
        private String roleName;
        @XmlElement(name="InstanceName")
        private String instanceName;
        @XmlElement(name="InstanceStatus")
        private String instanceStatus;
        @XmlElement(name="InstanceUpgradeDomain")
        private String instanceUpgradeDomain;
        @XmlElement(name="InstanceFaultDomain")
        private String instanceFaultDomain;
        @XmlElement(name="InstanceSize")
        private String instanceSize;
        @XmlElement(name="InstanceStateDetails")
        private String instanceStateDetails;
        @XmlElement(name="InstanceErrorCode")
        private String instanceErrorCode;
        @XmlElement(name="IpAddress")
        private String ipAddress;
        @XmlElementWrapper(name = "InstanceEndpoints")
        @XmlElement(name="InstanceEndpoint")
        private List<InstanceEndpoint> instanceEndpoints;
        @XmlElement(name="PowerState")
        private String powerState;
        @XmlElement(name="HostName")
        private String hostName;
        @XmlElement(name="RemoteAccessCertificateThumbprint")
        private String remoteAccessCertificateThumbprint;

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }

        public String getInstanceStatus() {
            return instanceStatus;
        }

        public void setInstanceStatus(String instanceStatus) {
            this.instanceStatus = instanceStatus;
        }

        public String getInstanceUpgradeDomain() {
            return instanceUpgradeDomain;
        }

        public void setInstanceUpgradeDomain(String instanceUpgradeDomain) {
            this.instanceUpgradeDomain = instanceUpgradeDomain;
        }

        public String getInstanceFaultDomain() {
            return instanceFaultDomain;
        }

        public void setInstanceFaultDomain(String instanceFaultDomain) {
            this.instanceFaultDomain = instanceFaultDomain;
        }

        public String getInstanceSize() {
            return instanceSize;
        }

        public void setInstanceSize(String instanceSize) {
            this.instanceSize = instanceSize;
        }

        public String getInstanceStateDetails() {
            return instanceStateDetails;
        }

        public void setInstanceStateDetails(String instanceStateDetails) {
            this.instanceStateDetails = instanceStateDetails;
        }

        public String getInstanceErrorCode() {
            return instanceErrorCode;
        }

        public void setInstanceErrorCode(String instanceErrorCode) {
            this.instanceErrorCode = instanceErrorCode;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public List<InstanceEndpoint> getInstanceEndpoints() {
            return instanceEndpoints;
        }

        public void setInstanceEndpoints(List<InstanceEndpoint> instanceEndpoints) {
            this.instanceEndpoints = instanceEndpoints;
        }

        public String getPowerState() {
            return powerState;
        }

        public void setPowerState(String powerState) {
            this.powerState = powerState;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public String getRemoteAccessCertificateThumbprint() {
            return remoteAccessCertificateThumbprint;
        }

        public void setRemoteAccessCertificateThumbprint(String remoteAccessCertificateThumbprint) {
            this.remoteAccessCertificateThumbprint = remoteAccessCertificateThumbprint;
        }
    }

    @XmlRootElement(name="InstanceEndpoint")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InstanceEndpoint {
        @XmlElement(name="Name")
        private String name;
        @XmlElement(name="Vip")
        private String vip;
        @XmlElement(name="PublicPort")
        private String publicPort;
        @XmlElement(name="LocalPort")
        private String localPort;
        @XmlElement(name="Protocol")
        private String protocol;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVip() {
            return vip;
        }

        public void setVip(String vip) {
            this.vip = vip;
        }

        public String getPublicPort() {
            return publicPort;
        }

        public void setPublicPort(String publicPort) {
            this.publicPort = publicPort;
        }

        public String getLocalPort() {
            return localPort;
        }

        public void setLocalPort(String localPort) {
            this.localPort = localPort;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }

    @XmlRootElement(name="HostedServiceProperties")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HostedServiceProperties {
        @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
        private String description;
        @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
        private String affinityGroup;
        @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
        private String location;
        @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
        private String label;
        @XmlElement(name="Status", namespace ="http://schemas.microsoft.com/windowsazure")
        private String status;
        @XmlElement(name="DateCreated", namespace ="http://schemas.microsoft.com/windowsazure")
        private String dateCreated;
        @XmlElement(name="DateLastModified", namespace ="http://schemas.microsoft.com/windowsazure")
        private String dateLastModified;
        @XmlElementWrapper(name = "ExtendedProperties")
        @XmlElement(name="ExtendedProperty")
        private List<ExtendedProperty> extendedProperties;
        @XmlElement(name="ReverseDnsFqdn", namespace ="http://schemas.microsoft.com/windowsazure")
        private String reverseDnsFqdn;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAffinityGroup() {
            return affinityGroup;
        }

        public void setAffinityGroup(String affinityGroup) {
            this.affinityGroup = affinityGroup;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(String dateCreated) {
            this.dateCreated = dateCreated;
        }

        public String getDateLastModified() {
            return dateLastModified;
        }

        public void setDateLastModified(String dateLastModified) {
            this.dateLastModified = dateLastModified;
        }

        public List<ExtendedProperty> getExtendedProperties() {
            return extendedProperties;
        }

        public void setExtendedProperties(List<ExtendedProperty> extendedProperties) {
            this.extendedProperties = extendedProperties;
        }

        public String getReverseDnsFqdn() {
            return reverseDnsFqdn;
        }

        public void setReverseDnsFqdn(String reverseDnsFqdn) {
            this.reverseDnsFqdn = reverseDnsFqdn;
        }
    }

    @XmlRootElement(name="ExtendedProperty")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ExtendedProperty {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;
        @XmlElement(name="Value", namespace ="http://schemas.microsoft.com/windowsazure")
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

