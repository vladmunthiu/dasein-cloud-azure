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

package org.dasein.cloud.azure.tests.compute.network;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.network.AzureIpAddressSupport;
import org.dasein.cloud.azure.network.AzureRuleIdParts;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertGet;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertPut;
import static org.junit.Assert.assertEquals;

/**
 * Created by Jeffrey Yan on 9/14/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureIpAddressSupportTest extends AzureTestsBase {

    @Test(expected = InternalException.class)
    public void forwardShouldThrowExceptionIfOnServerIdIsNull() throws CloudException, InternalException {
        AzureIpAddressSupport ipAddressSupport = new AzureIpAddressSupport(azureMock);
        ipAddressSupport.forward("127.0.0.1", 80, Protocol.TCP, 80, null);
    }

    @Test(expected = InternalException.class)
    public void forwardShouldThrowExceptionIfOnServerIdIsNotValid() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                String expectedUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roles/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);
                assertGet(request, expectedUrl, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null, new Header[]{});
            }
        };

        AzureIpAddressSupport ipAddressSupport = new AzureIpAddressSupport(azureMock);
        ipAddressSupport.forward("127.0.0.1", 80, Protocol.TCP, 80, VM_ID);
    }

    private PersistentVMRoleModel newPersistentVMRoleModel() {
        PersistentVMRoleModel persistentVMRoleModel = new PersistentVMRoleModel();
        List<PersistentVMRoleModel.ConfigurationSet> configurationSets = new ArrayList<PersistentVMRoleModel.ConfigurationSet>();
        configurationSets.add(new PersistentVMRoleModel.ConfigurationSet());
        persistentVMRoleModel.setConfigurationSets(configurationSets);
        return persistentVMRoleModel;
    }

    @Test
    public void forwardShouldPostCorrectRequest() throws CloudException, InternalException {
        final String vmUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roles/%s", ENDPOINT,
                ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);
        final int privatePort = 80;
        final int publicPort = 80;
        final Protocol protocol = Protocol.TCP;

        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if (request.getMethod().equals("GET")) {
                    DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(
                            newPersistentVMRoleModel());
                    assertGet(request, vmUrl, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity, new Header[] {});
                } else if(request.getMethod().equals("PUT")) {
                    PersistentVMRoleModel persistentVMRoleModel = newPersistentVMRoleModel();
                    List<PersistentVMRoleModel.InputEndpoint> inputEndpoints = new ArrayList<PersistentVMRoleModel.InputEndpoint>();
                    persistentVMRoleModel.getConfigurationSets().get(0).setInputEndpoints(inputEndpoints);
                    PersistentVMRoleModel.InputEndpoint inputEndpoint = new PersistentVMRoleModel.InputEndpoint();
                    inputEndpoint.setLocalPort(String.valueOf(privatePort));
                    inputEndpoint.setPort(String.valueOf(publicPort));
                    inputEndpoint.setProtocol(protocol.toString());
                    inputEndpoint.setName(protocol.toString() + String.valueOf(publicPort));
                    inputEndpoints.add(inputEndpoint);

                    assertPut(request, vmUrl, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            persistentVMRoleModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_ACCEPTED), null, new Header[] {});
                } else{
                    throw new IOException("Request is not mocked");
                }
            }
        };

        AzureIpAddressSupport ipAddressSupport = new AzureIpAddressSupport(azureMock);
        String result = ipAddressSupport.forward("127.0.0.1", publicPort, protocol, privatePort, VM_ID);
        assertEquals("IpAddressSupport.forward() doesn't return correct result",
                new AzureRuleIdParts(VM_ID, Protocol.TCP.toString(), String.valueOf(privatePort)).toProviderId(),
                result);
    }

//    @Test(expected = InternalException.class)
//    public void stopForwardToServerShouldThrowExceptionIfOnServerIdIsNull() throws CloudException, InternalException {
//        AzureIpAddressSupport ipAddressSupport = new AzureIpAddressSupport(azureMock);
//        ipAddressSupport.stopForwardToServer("127.0.0.1", 80, Protocol.TCP, 80, null);
//    }
//
//    @Test(expected = InternalException.class)
//    public void stopForwardToServerShouldThrowExceptionIfOnServerIdIsNotValid() throws CloudException, InternalException {
//        new MockUp<CloseableHttpClient>() {
//            @Mock(invocations = 1)
//            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
//                String expectedUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roles/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);
//                assertGet(request, expectedUrl, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
//                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null, new Header[]{});
//            }
//        };
//
//        AzureIpAddressSupport ipAddressSupport = new AzureIpAddressSupport(azureMock);
//        ipAddressSupport.stopForwardToServer("127.0.0.1", 80, Protocol.TCP, 80, VM_ID);
//    }
}
