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
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.model.AzureOperationStatus;
import org.dasein.cloud.azure.network.AzureVlanSupport;
import org.dasein.cloud.azure.network.model.NetworkConfigurationModel;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.azure.network.model.VirtualNetworkSitesModel;
import org.dasein.cloud.azure.tests.AzureTestsBaseWithLocation;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertGet;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertPut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        List<ResourceStatus> vlanStatuses = IteratorUtils.toList(vlanSupport.listVlanStatus().iterator());
        assertEquals("listVlanStatus should return 1 vlan", 1, vlanStatuses.size());
        assertEquals("listVlanStatus should return correct status", VLANState.AVAILABLE, vlanStatuses.get(0).getResourceStatus());
    }

    @Test
    public void listVlanStatusShouldReturnCorrectResult() throws CloudException, InternalException {
        new ListVlanNetworkSitesHttpClientMockUp();
        List<VLAN> vlans = IteratorUtils.toList(vlanSupport.listVlanStatus().iterator());
        assertEquals("listVlan should return 1 vlan", 1, vlans.size());
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
