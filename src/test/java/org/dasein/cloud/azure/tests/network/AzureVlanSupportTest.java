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

package org.dasein.cloud.azure.tests.network;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.collections.IteratorUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.azure.model.AzureOperationStatus;
import org.dasein.cloud.azure.network.AzureVlanSupport;
import org.dasein.cloud.azure.network.model.NetworkConfigurationModel;
import org.dasein.cloud.azure.network.model.VirtualNetworkSitesModel;
import org.dasein.cloud.azure.tests.AzureTestsBaseWithLocation;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertGet;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertPut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Created by Jeffrey Yan on 9/16/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureVlanSupportTest extends AzureTestsBaseWithLocation {

    private final String CIDR = "10.30.0.0/16";
    private final String ID = UUID.randomUUID().toString();
    private final String NAME = "vlan_name";
    private final String DESCRIPTION = "vlan_description";
    private final String DOMAIN_NAME = "vlan_domain_name";
    private final String[] DNS_SERVERS = { "dns_server1" };
    private final String[] NTP_SERVERS = { "ntp_server1" };

    private final String SUBNET_NAME = "subnet_name";
    private final String SUBNET_ID = SUBNET_NAME + "_" + ID;
    private final String SUBNET_CIDR = "10.30.255.0/24";

    private final String PUT_REQUEST_ID = UUID.randomUUID().toString();

    private final String NETWORK_CONFIG_URL = String.format("%s/%s/services/networking/media", ENDPOINT, ACCOUNT_NO);
    private final String OPERATION_STATUS_URL = String.format("%s/%s/operations/%s", ENDPOINT, ACCOUNT_NO, PUT_REQUEST_ID);
    private final String VIRTUAL_NETWORK_SITES_URL = String.format("%s/%s/services/networking/virtualnetwork", ENDPOINT, ACCOUNT_NO);

    private AzureVlanSupport vlanSupport;
    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();
        vlanSupport = new AzureVlanSupport(azureMock);
    }

    @Test(expected = InternalException.class)
    public void createVlanShouldThrowExceptionIfCidrIsNull() throws CloudException, InternalException {
        vlanSupport.createVlan(null, NAME, DESCRIPTION, DOMAIN_NAME, DNS_SERVERS, NTP_SERVERS);
    }

    @Test(expected = InternalException.class)
    public void createVlanShouldThrowExceptionIfCidrIsNotValid() throws CloudException, InternalException {
        vlanSupport.createVlan("10.0.0.0/4", NAME, DESCRIPTION, DOMAIN_NAME, DNS_SERVERS, NTP_SERVERS);
    }

    private void assertVlan(VLAN vlan, VLANState state) {
        assertEquals("Vlan id doesn't match", ID, vlan.getProviderVlanId());
        assertEquals("Vlan owner doesn't match", ACCOUNT_NO, vlan.getProviderOwnerId());
        assertEquals("Vlan name doesn't match", NAME, vlan.getName());
        assertEquals("Vlan region doesn't match", REGION, vlan.getProviderRegionId());
        assertEquals("Vlan datacenter doesn't amtch", REGION, vlan.getProviderDataCenterId());
        assertEquals("Vlan CIDR doesn't match", CIDR, vlan.getCidr());
        assertEquals("Vlan status doesn't match", state, vlan.getCurrentState());
    }

    @Test
    public void createVlanShouldPostCorrectRequestIfGetReturnNull() throws CloudException, InternalException {
        CreateVlanHttpClientMockUp mockUp = new CreateVlanHttpClientMockUp(){
            protected CloseableHttpResponse getNetworkConfigResponse() {
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            }
        };

        VLAN vlan = vlanSupport.createVlan(CIDR, NAME, DESCRIPTION, DOMAIN_NAME, DNS_SERVERS, NTP_SERVERS);
        assertVlan(vlan, VLANState.PENDING);
        assertEquals("createVlan PUT network config should perform only 1 times", 1, mockUp.putNetworkConfigCount);
    }

    @Test
    public void createVlanShouldPostCorrectRequest() throws CloudException, InternalException {
        CreateVlanHttpClientMockUp mockUp = new CreateVlanHttpClientMockUp();

        VLAN vlan = vlanSupport.createVlan(CIDR, NAME, DESCRIPTION, DOMAIN_NAME, DNS_SERVERS, NTP_SERVERS);
        assertVlan(vlan, VLANState.PENDING);
        assertEquals("createVlan PUT network config should perform only 1 times", 1, mockUp.putNetworkConfigCount);
    }

    @Test(expected = InternalException.class)
    public void removeVlanShouldThrowExceptionIfVlanIsNotExist() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        vlanSupport.removeVlan(UUID.randomUUID().toString());
    }

    @Test
    public void removeVlanShouldPostCorrectRequest() throws CloudException, InternalException {
        final AtomicInteger putCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET") && VIRTUAL_NETWORK_SITES_URL.equals(request.getURI().toString())) {
                    DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                            createVirtualNetworkSitesModel(ID, NAME, REGION, CIDR, "Updating"));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if("GET".equals(request.getMethod()) && NETWORK_CONFIG_URL.equals(request.getURI().toString())) {
                    DaseinObjectToXmlEntity<NetworkConfigurationModel> daseinEntity = new DaseinObjectToXmlEntity<NetworkConfigurationModel>(
                            createNetworkConfigurationModel(NAME, REGION, CIDR));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if("PUT".equals(request.getMethod())) {
                    putCount.incrementAndGet();
                    NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModel(null, null, null);
                    assertPut(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            networkConfigurationModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }

            }
        };
        vlanSupport.removeVlan(ID);
        assertEquals("removeVlan PUT network config should perform only 1 times", 1, putCount.get());
    }

    //TODO, create/remove VLAN when another VLAN is exist

    @Test
    public void getVlanShouldReturnNullIfVlanIsNotExist() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        VLAN vlan = vlanSupport.getVlan(UUID.randomUUID().toString());
        assertNull("getVlan should return null", vlan);
    }

    @Test
    public void getVlanShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        VLAN vlan = vlanSupport.getVlan(ID);
        assertVlan(vlan, VLANState.AVAILABLE);
    }

    @Test
    public void listVlanShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        List<VLAN> vlans = IteratorUtils.toList(vlanSupport.listVlans().iterator());
        assertEquals("listVlan should return 1 vlan", 1, vlans.size());
        assertVlan(vlans.get(0), VLANState.AVAILABLE);
    }

    @Test
    public void listVlanStatusShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        List<ResourceStatus> vlanStatuses = IteratorUtils.toList(vlanSupport.listVlanStatus().iterator());
        assertEquals("listVlanStatus should return 1 vlan", 1, vlanStatuses.size());
        assertEquals("listVlanStatus should return correct status", VLANState.AVAILABLE,
                vlanStatuses.get(0).getResourceStatus());
    }

    private void assertSubnet(Subnet subnet) {
        assertEquals("Subnet id doesn't match", SUBNET_ID, subnet.getProviderSubnetId());
        assertEquals("Subnet vlanId doesn't match", ID, subnet.getProviderVlanId());
        assertEquals("Subnet owner doesn't match", ACCOUNT_NO, subnet.getProviderOwnerId());
        assertEquals("Subnet name doesn't match", SUBNET_NAME, subnet.getName());
        assertEquals("Subnet region doesn't match", REGION, subnet.getProviderRegionId());
        assertEquals("Subnet datacenter doesn't amtch", REGION, subnet.getProviderDataCenterId());
        assertEquals("Subnet CIDR doesn't match", SUBNET_CIDR, subnet.getCidr());
        assertEquals("Subnet currentSate doesn't match", SubnetState.AVAILABLE, subnet.getCurrentState());
    }

    @Test(expected = NullPointerException.class)
    public void createSubnetShouldThrowExceptionIfVlanIsNotExist() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        SubnetCreateOptions subnetCreateOptions = SubnetCreateOptions.getInstance(UUID.randomUUID().toString(),
                SUBNET_CIDR, SUBNET_NAME, "description");
        vlanSupport.createSubnet(subnetCreateOptions);
    }

    @Test
    public void createSubnetShouldPostCorrectRequest() throws CloudException, InternalException {
        CreateSubnetHttpClientMockUp mockUp = new CreateSubnetHttpClientMockUp();
        SubnetCreateOptions subnetCreateOptions = SubnetCreateOptions
                .getInstance(ID, SUBNET_CIDR, SUBNET_NAME, "description");
        Subnet subnet = vlanSupport.createSubnet(subnetCreateOptions);
        assertSubnet(subnet);
        assertEquals("createSubnet PUT network config should perform only 1 times", 1, mockUp.putNetworkConfigCount);
    }

    @Test
    public void createSubnetShouldPostCorrectRequestIfAnotherSubnetIsExist() throws CloudException, InternalException {
        CreateSubnetHttpClientMockUp mockUp = new CreateSubnetHttpClientMockUp() {
            protected CloseableHttpResponse getNetworkConfigResponse() {
                DaseinObjectToXmlEntity<NetworkConfigurationModel> daseinEntity = new DaseinObjectToXmlEntity<NetworkConfigurationModel>(
                        createNetworkConfigurationModelWithSubnet(NAME, REGION, CIDR, "another_subnet", "10.30.10.0/24"));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            }

            protected CloseableHttpResponse putNetworkConfigResponse(HttpUriRequest request) {
                NetworkConfigurationModel.Subnet subnet = new NetworkConfigurationModel.Subnet();
                subnet.setName(SUBNET_NAME);
                subnet.setAddressPrefix(SUBNET_CIDR);
                NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModelWithSubnet(NAME,
                        REGION, CIDR, "another_subnet", "10.30.10.0/24");
                networkConfigurationModel.getVirtualNetworkConfiguration().getVirtualNetworkSites().get(0).getSubnets().add(subnet);
                assertPut(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                        networkConfigurationModel);
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                        new Header[] { new BasicHeader("x-ms-request-id", PUT_REQUEST_ID) });
            }
        };

        SubnetCreateOptions subnetCreateOptions = SubnetCreateOptions
                .getInstance(ID, SUBNET_CIDR, SUBNET_NAME, "description");
        Subnet subnet = vlanSupport.createSubnet(subnetCreateOptions);
        assertSubnet(subnet);
        assertEquals("createSubnet PUT network config should perform only 1 times", 1, mockUp.putNetworkConfigCount);
    }

    @Test(expected = NullPointerException.class)
    public void removeSubnetShouldThrowExceptionIfVlanIsNotExist() throws CloudException, InternalException {
        new ListSubnetNetworkSitesHttpClientMockUp();
        vlanSupport.removeSubnet(UUID.randomUUID().toString());
    }

    @Test
    public void removeSubnetShouldPostCorrectRequest() throws CloudException, InternalException {
        final AtomicInteger putCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET") && VIRTUAL_NETWORK_SITES_URL.equals(request.getURI().toString())) {
                    DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                            createVirtualNetworkSitesModelWithSubnet(ID, NAME, REGION, CIDR, "Created", SUBNET_NAME,
                                    SUBNET_CIDR));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if("GET".equals(request.getMethod()) && NETWORK_CONFIG_URL.equals(request.getURI().toString())) {
                    DaseinObjectToXmlEntity<NetworkConfigurationModel> daseinEntity = new DaseinObjectToXmlEntity<NetworkConfigurationModel>(
                            createNetworkConfigurationModelWithSubnet(NAME, REGION, CIDR, SUBNET_NAME, SUBNET_CIDR));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if("PUT".equals(request.getMethod())) {
                    putCount.incrementAndGet();
                    NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModelWithSubnet(NAME, REGION, CIDR, null, null);
                    assertPut(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            networkConfigurationModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }

            }
        };
        vlanSupport.removeSubnet(SUBNET_ID);
        assertEquals("removeVlan PUT network config should perform only 1 times", 1, putCount.get());
    }

    @Test
    public void listSubnetShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListSubnetNetworkSitesHttpClientMockUp();
        List<Subnet> subnets = IteratorUtils.toList(vlanSupport.listSubnets(ID).iterator());
        assertEquals("listSubnet should return 1 subnet", 1, subnets.size());
        assertSubnet(subnets.get(0));
    }

    @Test
    public void getSubnetShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListSubnetNetworkSitesHttpClientMockUp();
        Subnet subnet = vlanSupport.getSubnet(SUBNET_ID);
        assertSubnet(subnet);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void assignRoutingTableToSubnetShouldThrowException() throws CloudException, InternalException {
        vlanSupport.assignRoutingTableToSubnet(SUBNET_ID, "routingTableId");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void assignRoutingTableToVlanShouldThrowException() throws CloudException, InternalException {
        vlanSupport.assignRoutingTableToVlan(ID, "routingTableId");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void attachNetworkInterfaceShouldThrowException() throws CloudException, InternalException {
        vlanSupport.attachNetworkInterface("nicId", "vmId", 0);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void createInternetGatewayShouldThrowException() throws CloudException, InternalException {
        vlanSupport.createInternetGateway(ID);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void createRoutingTableShouldThrowException() throws CloudException, InternalException {
        vlanSupport.createRoutingTable(ID, NAME, "description");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void createNetworkInterfaceShouldThrowException() throws CloudException, InternalException {
        vlanSupport.createNetworkInterface(NICCreateOptions.getInstanceForSubnet(SUBNET_ID, SUBNET_NAME, "description"));
    }

    @Test
    public void getProviderTermForNetworkInterfaceShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("getProviderTermForNetworkInterface return doesn't match", "network interface",
                vlanSupport.getProviderTermForNetworkInterface(Locale.US));
    }

    @Test
    public void getProviderTermForSubnetShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("getProviderTermForSubnet return doesn't match", "Subnet", vlanSupport.getProviderTermForSubnet(
                Locale.US));
    }

    @Test
    public void getProviderTermForVlanShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("getProviderTermForVlan return doesn't match", "Address Space",
                vlanSupport.getProviderTermForVlan(Locale.US));
    }

    @Test
    public void getNetworkInterfaceShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("getNetworkInterface return doesn't match", vlanSupport.getNetworkInterface("nicId"));
    }

    @Test
    public void getRoutingTableForSubnetShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("getNetworkInterface return doesn't match", vlanSupport.getRoutingTableForSubnet(SUBNET_ID));
    }

    @Test
    public void getRoutingTableForVlanShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("getRoutingTableForVlan return doesn't match", vlanSupport.getRoutingTableForVlan(ID));
    }

    @Test
    public void isConnectedViaInternetGatewayShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("isConnectedViaInternetGateway return doesn't match", vlanSupport.isConnectedViaInternetGateway(ID));
    }

    @Test
    public void getAttachedInternetGatewayIdShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("getAttachedInternetGatewayId return doesn't match", vlanSupport.getAttachedInternetGatewayId(ID));
    }

    @Test
    public void getInternetGatewayByIdShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("getInternetGatewayById return doesn't match", vlanSupport.getInternetGatewayById("gatewayId"));
    }

    @Test
    public void listFirewallIdsForNICShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("listInternetGateways return doesn't match", vlanSupport.listInternetGateways("vlanId"));
    }

    @Test
    public void listNetworkInterfaceStatusShouldReturnCorrectResult() throws CloudException, InternalException {
        assertNull("listNetworkInterfaceStatus return doesn't match", vlanSupport.listNetworkInterfaceStatus());
    }

    @Test
    public void listNetworkInterfacesShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listNetworkInterfaces return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listNetworkInterfaces().iterator()).size());
    }

    @Test
    public void listNetworkInterfacesForVMShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listNetworkInterfacesForVM return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listNetworkInterfacesForVM(VM_ID).iterator()).size());
    }

    @Test
    public void listNetworkInterfacesInSubnetShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listNetworkInterfacesInSubnet return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listNetworkInterfacesInSubnet(SUBNET_ID).iterator()).size());
    }

    @Test
    public void listNetworkInterfacesInVLANShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listNetworkInterfacesInVLAN return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listNetworkInterfacesInVLAN(ID).iterator()).size());
    }

    @Test
    public void listResourcesShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listResources return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listResources(ID).iterator()).size());
    }

    @Test
    public void listRoutingTablesShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("listRoutingTables return doesn't match", 0,
                IteratorUtils.toList(vlanSupport.listRoutingTables(ID).iterator()).size());
    }

    @Test(expected = OperationNotSupportedException.class)
    public void removeInternetGatewayShouldThrowException() throws CloudException, InternalException {
        vlanSupport.removeInternetGateway(ID);
    }

    @Test
    public void removeInternetGatewayByIdShouldDoNothing() throws CloudException, InternalException {
        vlanSupport.removeInternetGatewayById("gatewatyId");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void removeNetworkInterfaceShouldThrowException() throws CloudException, InternalException {
        vlanSupport.removeNetworkInterface("nicId");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void removeRouteShouldThrowException() throws CloudException, InternalException {
        vlanSupport.removeRoute("routingTableId", "cidr");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void removeRoutingTableShouldThrowException() throws CloudException, InternalException {
        vlanSupport.removeRoutingTable("routingTableId");
    }

    private VirtualNetworkSitesModel createVirtualNetworkSitesModelWithSubnet(String id, String name, String location,
            String addressSpacePrefix, String state, String subnetName, String subnetAddressSpacePrefix) {
        VirtualNetworkSitesModel virtualNetworkSitesModel = createVirtualNetworkSitesModel(id, name, location, addressSpacePrefix, state);

        List<VirtualNetworkSitesModel.Subnet> subnets = new ArrayList<VirtualNetworkSitesModel.Subnet>();
        if (subnetName != null) {
            VirtualNetworkSitesModel.Subnet subnet = new VirtualNetworkSitesModel.Subnet();
            subnet.setName(subnetName);
            subnet.setAddressPrefix(subnetAddressSpacePrefix);
            subnets.add(subnet);
        }
        virtualNetworkSitesModel.getVirtualNetworkSites().get(0).setSubnets(subnets);

        return virtualNetworkSitesModel;
    }

    private VirtualNetworkSitesModel createVirtualNetworkSitesModel(String id, String name, String location, String addressSpacePrefix, String state) {
        VirtualNetworkSitesModel.VirtualNetworkSite virtualNetworkSite = new VirtualNetworkSitesModel.VirtualNetworkSite();
        virtualNetworkSite.setId(id);
        virtualNetworkSite.setName(name);
        virtualNetworkSite.setLocation(location);
        virtualNetworkSite.setState(state);
        VirtualNetworkSitesModel.AddressSpace addressSpace = new VirtualNetworkSitesModel.AddressSpace();
        List<String> addressPrefixes = new ArrayList<String>();
        addressPrefixes.add(addressSpacePrefix);
        addressSpace.setAddressPrefixes(addressPrefixes);
        virtualNetworkSite.setAddressSpace(addressSpace);

        VirtualNetworkSitesModel virtualNetworkSitesModel = new VirtualNetworkSitesModel();
        List<VirtualNetworkSitesModel.VirtualNetworkSite> virtualNetworkSites = new ArrayList<VirtualNetworkSitesModel.VirtualNetworkSite>();
        virtualNetworkSites.add(virtualNetworkSite);
        virtualNetworkSitesModel.setVirtualNetworkSites(virtualNetworkSites);
        return virtualNetworkSitesModel;
    }

    private AzureOperationStatus createAzureOperationStatus(String id, String status, int httpCode) {
        AzureOperationStatus azureOperationStatus = new AzureOperationStatus();
        azureOperationStatus.setId(id);
        azureOperationStatus.setStatus(status);
        azureOperationStatus.setHttpStatusCode(Integer.toString(httpCode));
        return azureOperationStatus;
    }

    private NetworkConfigurationModel createNetworkConfigurationModelWithSubnet(String name, String location,
            String addressSpacePrefix, String subnetName, String subnetAddressSpacePrefix) {
        NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModel(name, location, addressSpacePrefix);

        if (name != null) {
            List<NetworkConfigurationModel.Subnet> subnets = new ArrayList<NetworkConfigurationModel.Subnet>();
            if(subnetName != null) {
                NetworkConfigurationModel.Subnet subnet = new NetworkConfigurationModel.Subnet();
                subnet.setName(subnetName);
                subnet.setAddressPrefix(subnetAddressSpacePrefix);
                subnets.add(subnet);
            }
            networkConfigurationModel.getVirtualNetworkConfiguration().getVirtualNetworkSites().get(0).setSubnets(subnets);
        }

        return networkConfigurationModel;
    }

    private NetworkConfigurationModel createNetworkConfigurationModel(String name, String location, String addressSpacePrefix) {
        NetworkConfigurationModel networkConfigurationModel = new NetworkConfigurationModel();

        List<NetworkConfigurationModel.VirtualNetworkSite> virtualNetworkSites = new ArrayList<NetworkConfigurationModel.VirtualNetworkSite>();
        if(name != null) {
            NetworkConfigurationModel.VirtualNetworkSite virtualNetworkSite = new NetworkConfigurationModel.VirtualNetworkSite();
            virtualNetworkSite.setName(name);
            virtualNetworkSite.setLocation(location);
            NetworkConfigurationModel.AddressSpace addressSpace = new NetworkConfigurationModel.AddressSpace();
            addressSpace.setAddressPrefix(addressSpacePrefix);
            virtualNetworkSite.setAddressSpace(addressSpace);
            virtualNetworkSites.add(virtualNetworkSite);
        }

        NetworkConfigurationModel.VirtualNetworkConfiguration virtualNetworkConfiguration = new NetworkConfigurationModel.VirtualNetworkConfiguration();
        virtualNetworkConfiguration.setVirtualNetworkSites(virtualNetworkSites);
        NetworkConfigurationModel.Dns dns = new NetworkConfigurationModel.Dns();
        virtualNetworkConfiguration.setDns(dns);

        networkConfigurationModel.setVirtualNetworkConfiguration(virtualNetworkConfiguration);
        return networkConfigurationModel;
    }

    private class CreateSubnetHttpClientMockUp extends MockUp<CloseableHttpClient> {
        private int putNetworkConfigCount = 0;

        private int listVirtualNetworkSitesCount = 0;
        @Mock
        public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
            if (request.getMethod().equals("GET") && VIRTUAL_NETWORK_SITES_URL.equals(request.getURI().toString())) {
                if ((++listVirtualNetworkSitesCount) == 1) {
                    DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                            createVirtualNetworkSitesModel(ID, NAME, REGION, CIDR, "Created"));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                            createVirtualNetworkSitesModelWithSubnet(ID, NAME, REGION, CIDR, "Created", SUBNET_NAME, SUBNET_CIDR));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                }
            } else if("GET".equals(request.getMethod()) && NETWORK_CONFIG_URL.equals(request.getURI().toString())) {
                return getNetworkConfigResponse();
            } else if("PUT".equals(request.getMethod())) {
                putNetworkConfigCount++;
                return putNetworkConfigResponse(request);
            }  else if("GET".equals(request.getMethod()) && OPERATION_STATUS_URL.equals(request.getURI().toString())) {
                DaseinObjectToXmlEntity<AzureOperationStatus> daseinEntity = new DaseinObjectToXmlEntity<AzureOperationStatus>(
                        createAzureOperationStatus(PUT_REQUEST_ID, "Succeeded", 200));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else {
                throw new IOException("Request is not mocked");
            }
        }

        protected CloseableHttpResponse getNetworkConfigResponse() {
            DaseinObjectToXmlEntity<NetworkConfigurationModel> daseinEntity = new DaseinObjectToXmlEntity<NetworkConfigurationModel>(
                    createNetworkConfigurationModel(NAME, REGION, CIDR));
            return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                    new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
        }

        protected CloseableHttpResponse putNetworkConfigResponse(HttpUriRequest request) {
            NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModelWithSubnet(
                    NAME, REGION, CIDR, SUBNET_NAME, SUBNET_CIDR);
            assertPut(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                    networkConfigurationModel);
            return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                    new Header[] { new BasicHeader("x-ms-request-id", PUT_REQUEST_ID) });
        }
    }

    private class ListSubnetNetworkSitesHttpClientMockUp extends MockUp<CloseableHttpClient> {
        @Mock
        public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
            if (request.getMethod().equals("GET")) {
                DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                        createVirtualNetworkSitesModelWithSubnet(ID, NAME, REGION, CIDR, "Created", SUBNET_NAME,
                                SUBNET_CIDR));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else {
                throw new IOException("Request is not mocked");
            }
        }
    }

    private class ListVlanNetworkSitesHttpClientMockUp extends MockUp<CloseableHttpClient> {
        @Mock
        public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
            if (request.getMethod().equals("GET")) {
                DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                        createVirtualNetworkSitesModel(ID, NAME, REGION, CIDR, "Created"));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else {
                throw new IOException("Request is not mocked");
            }
        }
    }

    private class CreateVlanHttpClientMockUp extends MockUp<CloseableHttpClient> {
        private int putNetworkConfigCount = 0;
        @Mock
        public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
            if("GET".equals(request.getMethod()) && NETWORK_CONFIG_URL.equals(request.getURI().toString())) {
                assertGet(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                return getNetworkConfigResponse();
            } else if("PUT".equals(request.getMethod())) {
                putNetworkConfigCount++;

                NetworkConfigurationModel networkConfigurationModel = createNetworkConfigurationModel(NAME, REGION, CIDR);
                assertPut(request, NETWORK_CONFIG_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                        networkConfigurationModel);
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                        new Header[] { new BasicHeader("x-ms-request-id", PUT_REQUEST_ID) });
            } else if("GET".equals(request.getMethod()) && OPERATION_STATUS_URL.equals(request.getURI().toString())) {
                DaseinObjectToXmlEntity<AzureOperationStatus> daseinEntity = new DaseinObjectToXmlEntity<AzureOperationStatus>(
                        createAzureOperationStatus(PUT_REQUEST_ID, "Succeeded", 200));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else if("GET".equals(request.getMethod()) && VIRTUAL_NETWORK_SITES_URL.equals(request.getURI().toString())) {
                DaseinObjectToXmlEntity<VirtualNetworkSitesModel> daseinEntity = new DaseinObjectToXmlEntity<VirtualNetworkSitesModel>(
                        createVirtualNetworkSitesModel(ID, NAME, REGION, CIDR, "Creating"));
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else {
                throw new IOException("Request is not mocked");
            }
        }

        protected CloseableHttpResponse getNetworkConfigResponse() {
            DaseinObjectToXmlEntity<NetworkConfigurationModel> daseinEntity = new DaseinObjectToXmlEntity<NetworkConfigurationModel>(
                    createNetworkConfigurationModel(null, null, null));
            return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                    new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
        }
    }
}
