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
import org.dasein.cloud.azure.network.AzureIpAddressSupport;
import org.dasein.cloud.azure.network.AzureRuleIdParts;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.Protocol;
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
import static org.junit.Assert.assertTrue;

/**
 * Created by Jeffrey Yan on 9/14/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureIpAddressSupportTest extends AzureTestsBase {

    private final int PRIVATE_PORT = 80;
    private final int PUBLIC_PORT = 80;
    private final Protocol PROTOCOL = Protocol.TCP;
    private final String EXPECTED_URL = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roles/%s",
            ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

    private AzureIpAddressSupport ipAddressSupport;
    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();
        ipAddressSupport = new AzureIpAddressSupport(azureMock);
    }

    @Test(expected = InternalException.class)
    public void forwardShouldThrowExceptionIfOnServerIdIsNull() throws CloudException, InternalException {
        ipAddressSupport.forward("127.0.0.1", PUBLIC_PORT, PROTOCOL, PRIVATE_PORT, null);
    }

    @Test(expected = InternalException.class)
    public void forwardShouldThrowExceptionIfOnServerIdIsNotValid() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            }
        };

        ipAddressSupport.forward("127.0.0.1", PUBLIC_PORT, PROTOCOL, PRIVATE_PORT, VM_ID);
    }

    @Test
    public void forwardShouldPostCorrectRequest() throws CloudException, InternalException {
        final AtomicInteger putCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET")) {
                    DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(
                            createPersistentVMRoleModelWithoutEndpoint());
                    assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if(request.getMethod().equals("PUT")) {
                    putCount.incrementAndGet();
                    PersistentVMRoleModel persistentVMRoleModel = createPersistentVMRoleModelWithEndpoint();
                    assertPut(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            persistentVMRoleModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_ACCEPTED), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else{
                    throw new IOException("Request is not mocked");
                }
            }
        };

        String result = ipAddressSupport.forward("127.0.0.1", PUBLIC_PORT, PROTOCOL, PRIVATE_PORT, VM_ID);
        assertEquals("IpAddressSupport.forward() doesn't return correct result",
                new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT)).toProviderId(),
                result);
        assertEquals("PUT count doesn't match", 1, putCount.get());
    }

    @Test(expected = InternalException.class)
    public void stopForwardToServerShouldThrowExceptionIfOnServerIdIsNull() throws CloudException, InternalException {
        String ruleId = new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT)).toProviderId();
        ipAddressSupport.stopForwardToServer(ruleId, null);
    }

    @Test(expected = InternalException.class)
    public void stopForwardToServerShouldThrowExceptionIfRuleIdIsNull() throws CloudException, InternalException {
        ipAddressSupport.stopForwardToServer(null, VM_ID);
    }

    @Test(expected = InternalException.class)
    public void stopForwardToServerShouldThrowExceptionIfOnServerIdIsNotValid() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            }
        };

        String ruleId = new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT)).toProviderId();
        ipAddressSupport.stopForwardToServer(ruleId, VM_ID);
    }

    @Test(expected = InternalException.class)
    public void stopForwardToServerShouldThrowExceptionIfRuleIdIsNotValid() throws CloudException, InternalException {
        ipAddressSupport.stopForwardToServer("NotValidRuleId", VM_ID);
    }

    @Test
    public void stopForwardToServerShouldPostCorrectRequest() throws CloudException, InternalException {
        final AtomicInteger putCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET")) {
                    DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(
                            createPersistentVMRoleModelWithEndpoint());
                    assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if(request.getMethod().equals("PUT")) {
                    putCount.incrementAndGet();
                    PersistentVMRoleModel persistentVMRoleModel = createPersistentVMRoleModelWithoutEndpoint();
                    //set an empty list otherwise unitils will assert fail as one is null while another is empty list
                    persistentVMRoleModel.getConfigurationSets().get(0).setInputEndpoints(new ArrayList<PersistentVMRoleModel.InputEndpoint>());
                    assertPut(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            persistentVMRoleModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_ACCEPTED), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else{
                    throw new IOException("Request is not mocked");
                }
            }
        };
        String ruleId = new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT)).toProviderId();
        ipAddressSupport.stopForwardToServer(ruleId, VM_ID);
        assertEquals("PUT count doesn't match", 1, putCount.get());
    }

    @Test
    public void stopForwardToServerShouldPostCorrectRequestIfNoMatchEndpointFound() throws CloudException, InternalException {
        final AtomicInteger putCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET")) {
                    DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(
                            createPersistentVMRoleModelWithEndpoint());
                    assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if(request.getMethod().equals("PUT")) {
                    putCount.incrementAndGet();
                    PersistentVMRoleModel persistentVMRoleModel = createPersistentVMRoleModelWithEndpoint();
                    assertPut(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            persistentVMRoleModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_ACCEPTED), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        String ruleId = new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT + 1)).toProviderId();
        ipAddressSupport.stopForwardToServer(ruleId, VM_ID);
        assertEquals("PUT count doesn't match", 1, putCount.get());
    }

    @Test(expected = InternalException.class)
    public void listRulesForServerShouldThrowExceptionIfOnServerIdIsNull() throws CloudException, InternalException {
        ipAddressSupport.listRulesForServer(null);
    }

    @Test(expected = InternalException.class)
    public void listRulesForServerShouldThrowExceptionIfOnServerIdIsNotValid() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            }
        };

        ipAddressSupport.listRulesForServer(VM_ID);
    }

    @Test
    public void listRulesForServerShouldReturnCorrectResult() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET")) {
                    DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(
                            createPersistentVMRoleModelWithEndpoint());
                    assertGet(request, EXPECTED_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        List<IpForwardingRule> rules = IteratorUtils.toList(ipAddressSupport.listRulesForServer(VM_ID).iterator());
        assertEquals("listRulesForServer doesn't return correct rule size", 1, rules.size());
        IpForwardingRule rule = rules.get(0);
        assertEquals("listRulesForServer doesn't return correct rule", PRIVATE_PORT, rule.getPrivatePort());
        assertEquals("listRulesForServer doesn't return correct rule", PUBLIC_PORT, rule.getPublicPort());
        assertEquals("listRulesForServer doesn't return correct rule", PROTOCOL, rule.getProtocol());
        assertEquals("listRulesForServer doesn't return correct rule", VM_ID, rule.getServerId());
        String ruleId = new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(PRIVATE_PORT)).toProviderId();
        assertEquals("listRulesForServer doesn't return correct rule", ruleId, rule.getProviderRuleId());
    }

    private PersistentVMRoleModel createPersistentVMRoleModelWithoutEndpoint() {
        PersistentVMRoleModel persistentVMRoleModel = new PersistentVMRoleModel();
        List<PersistentVMRoleModel.ConfigurationSet> configurationSets = new ArrayList<PersistentVMRoleModel.ConfigurationSet>();
        configurationSets.add(new PersistentVMRoleModel.ConfigurationSet());
        persistentVMRoleModel.setConfigurationSets(configurationSets);
        return persistentVMRoleModel;
    }

    private PersistentVMRoleModel createPersistentVMRoleModelWithEndpoint() {
        PersistentVMRoleModel persistentVMRoleModel = createPersistentVMRoleModelWithoutEndpoint();
        List<PersistentVMRoleModel.InputEndpoint> inputEndpoints = new ArrayList<PersistentVMRoleModel.InputEndpoint>();
        persistentVMRoleModel.getConfigurationSets().get(0).setInputEndpoints(inputEndpoints);
        PersistentVMRoleModel.InputEndpoint inputEndpoint = new PersistentVMRoleModel.InputEndpoint();
        inputEndpoint.setLocalPort(String.valueOf(PRIVATE_PORT));
        inputEndpoint.setPort(String.valueOf(PUBLIC_PORT));
        inputEndpoint.setProtocol(PROTOCOL.toString());
        inputEndpoint.setName(PROTOCOL.toString() + String.valueOf(PUBLIC_PORT));
        inputEndpoints.add(inputEndpoint);
        return persistentVMRoleModel;
    }

    @Test(expected = OperationNotSupportedException.class)
    public void assignShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.assign("127.0.0.1", VM_ID);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void assignToNetworkInterfaceShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.assignToNetworkInterface("127.0.0.1", "nic_id");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void getIpAddressShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.getIpAddress("127.0.0.1");
    }

    @Test
    public void getProviderTermForIpAddressShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("getProviderTermForIpAddress doesn't return correct result", "IP Address",
                ipAddressSupport.getProviderTermForIpAddress(Locale.ENGLISH));
    }

    @Test(expected = OperationNotSupportedException.class)
    public void identifyVlanForVlanIPRequirementShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.identifyVlanForVlanIPRequirement();
    }

    @Test
    public void isAssignedShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("isAssigned doesn't return correct result", ipAddressSupport.isAssigned(AddressType.PRIVATE));
        assertFalse("isAssigned doesn't return correct result", ipAddressSupport.isAssigned(AddressType.PUBLIC));

        assertFalse("isAssigned doesn't return correct result", ipAddressSupport.isAssigned(IPVersion.IPV4));
        assertFalse("isAssigned doesn't return correct result", ipAddressSupport.isAssigned(IPVersion.IPV6));
    }

    @Test
    public void isAssignablePostLaunchShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("isAssignablePostLaunch doesn't return correct result",
                ipAddressSupport.isAssignablePostLaunch(IPVersion.IPV4));
        assertFalse("isAssignablePostLaunch doesn't return correct result",
                ipAddressSupport.isAssignablePostLaunch(IPVersion.IPV6));
    }

    @Test
    public void isForwardingShouldReturnCorrectResult() throws CloudException, InternalException {
        assertTrue("isForwarding doesn't return correct result", ipAddressSupport.isForwarding());

        assertTrue("isForwarding doesn't return correct result", ipAddressSupport.isForwarding(IPVersion.IPV4));
        assertTrue("isForwarding doesn't return correct result", ipAddressSupport.isForwarding(IPVersion.IPV6));
    }

    @Test
    public void isRequestableShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("isRequestable doesn't return correct result", ipAddressSupport.isRequestable(AddressType.PRIVATE));
        assertFalse("isRequestable doesn't return correct result", ipAddressSupport.isRequestable(AddressType.PUBLIC));

        assertFalse("isRequestable doesn't return correct result", ipAddressSupport.isRequestable(IPVersion.IPV4));
        assertFalse("isRequestable doesn't return correct result", ipAddressSupport.isRequestable(IPVersion.IPV6));
    }

    @Test
    public void isSubscribedShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("isSubscribed doesn't return correct result", ipAddressSupport.isSubscribed());
    }

    @Test
    public void listPrivateIpPoolShouldReturnCorrectResult() throws CloudException, InternalException {
        List<IpAddress> ipAddresses = IteratorUtils.toList(ipAddressSupport.listPrivateIpPool(false).iterator());
        assertEquals("listPrivateIpPool doesn't return correct result", 0, ipAddresses.size());
    }

    @Test
    public void listPublicIpPoolShouldReturnCorrectResult() throws CloudException, InternalException {
        List<IpAddress> ipAddresses = IteratorUtils.toList(ipAddressSupport.listPublicIpPool(false).iterator());
        assertEquals("listPublicIpPool doesn't return correct result", 0, ipAddresses.size());
    }

    @Test
    public void listIpPoolShouldReturnCorrectResult() throws CloudException, InternalException {
        List<IpAddress> ipAddresses = IteratorUtils.toList(ipAddressSupport.listIpPool(IPVersion.IPV4, false).iterator());
        assertEquals("listIpPool doesn't return correct result", 0, ipAddresses.size());
    }

    @Test
    public void listIpPoolConcurrentlyShouldReturnNull() throws CloudException, InternalException {
        assertNull("listIpPoolConcurrently doesn't return null",
                ipAddressSupport.listIpPoolConcurrently(IPVersion.IPV4, false));
    }

    @Test
    public void listIpPoolStatusShouldReturnCorrectResult() throws CloudException, InternalException {
        List<ResourceStatus> resourceStatuses = IteratorUtils.toList(ipAddressSupport.listIpPoolStatus(IPVersion.IPV4).iterator());
        assertEquals("listIpPoolStatus doesn't return correct result", 0, resourceStatuses.size());
    }

    @Test
    public void listSupportedIPVersionsShouldReturnCorrectResult() throws CloudException, InternalException {
        List<IPVersion> ipVersions = IteratorUtils.toList(ipAddressSupport.listSupportedIPVersions().iterator());
        assertEquals("listSupportedIPVersions doesn't return null", 0, ipVersions.size());
    }

    @Test(expected = OperationNotSupportedException.class)
    public void releaseFromPoolShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.releaseFromPool("127.0.0.1");
    }

    @Test(expected = OperationNotSupportedException.class)
    public void releaseFromServerShouldThrowException() throws CloudException, InternalException {
        ipAddressSupport.releaseFromServer(VM_ID);
    }

    @Test
    public void requestShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("request doesn't return correct result", "", ipAddressSupport.request(AddressType.PRIVATE));
        assertEquals("request doesn't return correct result", "", ipAddressSupport.request(AddressType.PUBLIC));

        assertEquals("request doesn't return correct result", "", ipAddressSupport.request(IPVersion.IPV4));
    }

    @Test
    public void requestForVLANShouldReturnCorrectResult() throws CloudException, InternalException {
        assertEquals("requestForVLAN doesn't return correct result", "", ipAddressSupport.requestForVLAN(IPVersion.IPV4));

        assertEquals("requestForVLAN doesn't return correct result", "", ipAddressSupport.requestForVLAN(IPVersion.IPV4,
                "vlan_id"));
    }

    @Test
    public void supportsVLANAddressesShouldReturnCorrectResult() throws CloudException, InternalException {
        assertFalse("supportsVLANAddresses doesn't return correct result", ipAddressSupport.supportsVLANAddresses(IPVersion.IPV4));
    }
}
