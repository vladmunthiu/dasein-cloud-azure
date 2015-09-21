/*
 *  *
 *  Copyright (C) 2009-2015 Dell, Inc.
 *  See annotations for authorship information
 *
 *  ====================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ====================================================================
 *
 */

package org.dasein.cloud.azure.network.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Jeffrey Yan on 9/16/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
@XmlRootElement(name="NetworkConfiguration", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class NetworkConfigurationModel {
    @XmlElement(name="VirtualNetworkConfiguration", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
    private VirtualNetworkConfiguration virtualNetworkConfiguration;

    public VirtualNetworkConfiguration getVirtualNetworkConfiguration() {
        return virtualNetworkConfiguration;
    }

    public void setVirtualNetworkConfiguration(VirtualNetworkConfiguration virtualNetworkConfiguration) {
        this.virtualNetworkConfiguration = virtualNetworkConfiguration;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VirtualNetworkConfiguration {
        @XmlElement(name="Dns", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private Dns dns;

        @XmlElementWrapper(name = "LocalNetworkSites", namespace = "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        @XmlElement(name="LocalNetworkSite", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private List<LocalNetworkSite> localNetworkSites;

        @XmlElementWrapper(name = "VirtualNetworkSites", namespace = "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        @XmlElement(name="VirtualNetworkSite", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private List<VirtualNetworkSite> virtualNetworkSites;

        public Dns getDns() {
            return dns;
        }

        public void setDns(Dns dns) {
            this.dns = dns;
        }

        public List<LocalNetworkSite> getLocalNetworkSites() {
            return localNetworkSites;
        }

        public void setLocalNetworkSites(List<LocalNetworkSite> localNetworkSites) {
            this.localNetworkSites = localNetworkSites;
        }

        public List<VirtualNetworkSite> getVirtualNetworkSites() {
            return virtualNetworkSites;
        }

        public void setVirtualNetworkSites(List<VirtualNetworkSite> virtualNetworkSites) {
            this.virtualNetworkSites = virtualNetworkSites;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Dns {
        @XmlElementWrapper(name = "DnsServers", namespace = "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        @XmlElement(name="DnsServer", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private List<DnsServer> dnsServers;

        public List<DnsServer> getDnsServers() {
            return dnsServers;
        }

        public void setDnsServers(List<DnsServer> dnsServers) {
            this.dnsServers = dnsServers;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DnsServer {
        @XmlAttribute(name="name")
        private String name;

        @XmlAttribute(name="IPAddress")
        private String ipAddress;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalNetworkSite {
        @XmlAttribute(name="name")
        private String name;

        @XmlElement(name="VPNGatewayAddress", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private String vpnGatewayAddress;

        @XmlElement(name="AddressSpace", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private AddressSpace addressSpace;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVpnGatewayAddress() {
            return vpnGatewayAddress;
        }

        public void setVpnGatewayAddress(String vpnGatewayAddress) {
            this.vpnGatewayAddress = vpnGatewayAddress;
        }

        public AddressSpace getAddressSpace() {
            return addressSpace;
        }

        public void setAddressSpace(AddressSpace addressSpace) {
            this.addressSpace = addressSpace;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AddressSpace {
        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private String addressPrefix;

        public String getAddressPrefix() {
            return addressPrefix;
        }

        public void setAddressPrefix(String addressPrefix) {
            this.addressPrefix = addressPrefix;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VirtualNetworkSite {
        @XmlAttribute(name="name")
        private String name;

        @XmlAttribute(name="AffinityGroup")
        private String affinityGroup;

        @XmlAttribute(name="Location")
        private String location;

        @XmlElement(name="Gateway", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private Gateway gateway;

        @XmlElementWrapper(name = "DnsServersRef", namespace = "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        @XmlElement(name="DnsServerRef", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private List<DnsServerRef> dnsServerRef;

        @XmlElementWrapper(name = "Subnets", namespace = "http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        @XmlElement(name="Subnet", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private List<Subnet> subnets;

        @XmlElement(name="AddressSpace", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private AddressSpace addressSpace;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public Gateway getGateway() {
            return gateway;
        }

        public void setGateway(Gateway gateway) {
            this.gateway = gateway;
        }

        public List<DnsServerRef> getDnsServerRef() {
            return dnsServerRef;
        }

        public void setDnsServerRef(List<DnsServerRef> dnsServerRef) {
            this.dnsServerRef = dnsServerRef;
        }

        public List<Subnet> getSubnets() {
            return subnets;
        }

        public void setSubnets(List<Subnet> subnets) {
            this.subnets = subnets;
        }

        public AddressSpace getAddressSpace() {
            return addressSpace;
        }

        public void setAddressSpace(AddressSpace addressSpace) {
            this.addressSpace = addressSpace;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Gateway {
        @XmlElement(name="VPNClientAddressPool", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private VPNClientAddressPool vpnClientAddressPool;

        @XmlElement(name="ConnectionsToLocalNetwork", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private ConnectionsToLocalNetwork connectionsToLocalNetwork;

        public VPNClientAddressPool getVpnClientAddressPool() {
            return vpnClientAddressPool;
        }

        public void setVpnClientAddressPool(VPNClientAddressPool vpnClientAddressPool) {
            this.vpnClientAddressPool = vpnClientAddressPool;
        }

        public ConnectionsToLocalNetwork getConnectionsToLocalNetwork() {
            return connectionsToLocalNetwork;
        }

        public void setConnectionsToLocalNetwork(ConnectionsToLocalNetwork connectionsToLocalNetwork) {
            this.connectionsToLocalNetwork = connectionsToLocalNetwork;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VPNClientAddressPool {
        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private String addressPrefix;

        public String getAddressPrefix() {
            return addressPrefix;
        }

        public void setAddressPrefix(String addressPrefix) {
            this.addressPrefix = addressPrefix;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ConnectionsToLocalNetwork {
        @XmlElement(name="LocalNetworkSiteRef", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private LocalNetworkSiteRef localNetworkSiteRef;

        public LocalNetworkSiteRef getLocalNetworkSiteRef() {
            return localNetworkSiteRef;
        }

        public void setLocalNetworkSiteRef(LocalNetworkSiteRef localNetworkSiteRef) {
            this.localNetworkSiteRef = localNetworkSiteRef;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalNetworkSiteRef {
        @XmlAttribute(name="name")
        private String name;

        @XmlElement(name="Connection", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private Connection connection;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Connection {
        @XmlAttribute(name="type")
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DnsServerRef {
        @XmlAttribute(name="name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Subnet {
        @XmlAttribute(name="name")
        private String name;

        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration")
        private String addressPrefix;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddressPrefix() {
            return addressPrefix;
        }

        public void setAddressPrefix(String addressPrefix) {
            this.addressPrefix = addressPrefix;
        }
    }
}
