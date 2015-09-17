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
@XmlRootElement(name="VirtualNetworkSites", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class VirtualNetworkSitesModel {
    @XmlElement(name="VirtualNetworkSite", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<VirtualNetworkSite> virtualNetworkSites;

    public List<VirtualNetworkSite> getVirtualNetworkSites() {
        return virtualNetworkSites;
    }

    public void setVirtualNetworkSites(List<VirtualNetworkSite> virtualNetworkSites) {
        this.virtualNetworkSites = virtualNetworkSites;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VirtualNetworkSite {
        @XmlElement(name="Id", namespace ="http://schemas.microsoft.com/windowsazure")
        private String id;

        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;

        @XmlElement(name="Label", namespace ="http://schemas.microsoft.com/windowsazure")
        private String label;

        @XmlElement(name="AffinityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
        private String affinityGroup;

        @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
        private String location;

        @XmlElement(name="State", namespace ="http://schemas.microsoft.com/windowsazure")
        private String state;

        @XmlElement(name="AddressSpace", namespace ="http://schemas.microsoft.com/windowsazure")
        private AddressSpace addressSpace;

        @XmlElementWrapper(name = "Subnets", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="Subnet", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<Subnet> subnets;

        @XmlElementWrapper(name = "DnsServers", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="DnsServer", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<DnsServer> dnsServers;

        @XmlElement(name="Gateway", namespace ="http://schemas.microsoft.com/windowsazure")
        private Gateway gateway;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public AddressSpace getAddressSpace() {
            return addressSpace;
        }

        public void setAddressSpace(AddressSpace addressSpace) {
            this.addressSpace = addressSpace;
        }

        public List<Subnet> getSubnets() {
            return subnets;
        }

        public void setSubnets(List<Subnet> subnets) {
            this.subnets = subnets;
        }

        public List<DnsServer> getDnsServers() {
            return dnsServers;
        }

        public void setDnsServers(List<DnsServer> dnsServers) {
            this.dnsServers = dnsServers;
        }

        public Gateway getGateway() {
            return gateway;
        }

        public void setGateway(Gateway gateway) {
            this.gateway = gateway;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AddressSpace {
        @XmlElementWrapper(name = "AddressPrefixes", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<String> addressPrefixes;

        public List<String> getAddressPrefixes() {
            return addressPrefixes;
        }

        public void setAddressPrefixes(List<String> addressPrefixes) {
            this.addressPrefixes = addressPrefixes;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Subnet {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;

        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/windowsazure")
        private String addressPrefix;

        @XmlElement(name="NetworkSecurityGroup", namespace ="http://schemas.microsoft.com/windowsazure")
        private String networkSecurityGroup;

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

        public String getNetworkSecurityGroup() {
            return networkSecurityGroup;
        }

        public void setNetworkSecurityGroup(String networkSecurityGroup) {
            this.networkSecurityGroup = networkSecurityGroup;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DnsServer {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;

        @XmlElement(name="Address", namespace ="http://schemas.microsoft.com/windowsazure")
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

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Gateway {
        @XmlElement(name="Profile", namespace ="http://schemas.microsoft.com/windowsazure")
        private String profile;

        @XmlElementWrapper(name = "Sites", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="LocalNetworkSite", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<LocalNetworkSite> sites;

        @XmlElement(name="VPNClientAddressPool", namespace ="http://schemas.microsoft.com/windowsazure")
        private VPNClientAddressPool vpnClientAddressPool;

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public List<LocalNetworkSite> getSites() {
            return sites;
        }

        public void setSites(List<LocalNetworkSite> sites) {
            this.sites = sites;
        }

        public VPNClientAddressPool getVpnClientAddressPool() {
            return vpnClientAddressPool;
        }

        public void setVpnClientAddressPool(VPNClientAddressPool vpnClientAddressPool) {
            this.vpnClientAddressPool = vpnClientAddressPool;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalNetworkSite {
        @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
        private String name;

        @XmlElement(name="AddressSpace", namespace ="http://schemas.microsoft.com/windowsazure")
        private AddressSpace addressSpace;

        @XmlElement(name="VpnGatewayAddress", namespace ="http://schemas.microsoft.com/windowsazure")
        private String vpnGatewayAddress;

        @XmlElementWrapper(name = "Connections", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="Connection", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<Connection> connections;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AddressSpace getAddressSpace() {
            return addressSpace;
        }

        public void setAddressSpace(AddressSpace addressSpace) {
            this.addressSpace = addressSpace;
        }

        public String getVpnGatewayAddress() {
            return vpnGatewayAddress;
        }

        public void setVpnGatewayAddress(String vpnGatewayAddress) {
            this.vpnGatewayAddress = vpnGatewayAddress;
        }

        public List<Connection> getConnections() {
            return connections;
        }

        public void setConnections(List<Connection> connections) {
            this.connections = connections;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Connection {
        @XmlElement(name="Type", namespace ="http://schemas.microsoft.com/windowsazure")
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VPNClientAddressPool {
        @XmlElementWrapper(name = "AddressPrefixes", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="AddressPrefix", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<String> addressPrefixes;

        public List<String> getAddressPrefixes() {
            return addressPrefixes;
        }

        public void setAddressPrefixes(List<String> addressPrefixes) {
            this.addressPrefixes = addressPrefixes;
        }
    }
}
