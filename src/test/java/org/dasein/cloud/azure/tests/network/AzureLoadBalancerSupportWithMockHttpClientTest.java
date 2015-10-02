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
import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.apache.commons.collections.IteratorUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.AzureException;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.model.AzureOperationStatus;
import org.dasein.cloud.azure.network.AzureLoadBalancerSupport;
import org.dasein.cloud.azure.network.model.DefinitionModel;
import org.dasein.cloud.azure.network.model.ProfileModel;
import org.dasein.cloud.azure.network.model.ProfilesModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.network.HealthCheckFilterOptions;
import org.dasein.cloud.network.HealthCheckOptions;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbEndpointState;
import org.dasein.cloud.network.LbEndpointType;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LbPersistence;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerAddressType;
import org.dasein.cloud.network.LoadBalancerCreateOptions;
import org.dasein.cloud.network.LoadBalancerEndpoint;
import org.dasein.cloud.network.LoadBalancerHealthCheck;
import org.dasein.cloud.network.LoadBalancerState;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertDelete;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertGet;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertPost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by Jeffrey Yan on 9/21/2015.
 *
 * @author Jeffrey Yan
 * @since 2015.09.1
 */
public class AzureLoadBalancerSupportWithMockHttpClientTest extends AzureTestsBase {

    private final String LB_NAME = "lb_name";
    private final String LB_DESCRIPTION = "lb_description";
    private final String LB_DOMAIN = String.format("%s.%s", LB_NAME, "trafficmanager.net");
    private final int LB_PUBLIC_PORT=80;
    private final int LB_PRIVATE_PORT = 80;
    private final LbProtocol LB_PROTOCOL = LbProtocol.HTTP;

    private final String HC_DESCRIPTION = "hc_description";
    private final LoadBalancerHealthCheck.HCProtocol HC_PROTOCOL = LoadBalancerHealthCheck.HCProtocol.HTTP;
    private final int HC_PORT = 80;
    private final String HC_PATH = "/";

    private final String PROFILES_URL = String.format("%s/%s/services/WATM/profiles", ENDPOINT, ACCOUNT_NO);
    private final String PROFILE_URL = String.format("%s/%s/services/WATM/profiles/%s", ENDPOINT, ACCOUNT_NO, LB_NAME);
    private final String DEFINITIONS_URL = String.format("%s/%s/services/WATM/profiles/%s/definitions", ENDPOINT, ACCOUNT_NO, LB_NAME);
    private final String DEFINITION_URL = String.format("%s/%s/services/WATM/profiles/%s/definitions/1", ENDPOINT, ACCOUNT_NO, LB_NAME);

    private AzureLoadBalancerSupport loadBalancerSupport;

    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();
        loadBalancerSupport = new AzureLoadBalancerSupport(azureMock);
    }

    @Test(expected = InternalException.class)
    public void createLoadBalancerShouldThrowExceptionIfHealthCheckOptionsIsNull() throws CloudException, InternalException {
        loadBalancerSupport.createLoadBalancer(LoadBalancerCreateOptions.getInstance(LB_NAME, LB_DESCRIPTION));
    }

    @Test(expected = InternalException.class)
    public void createLoadBalancerShouldThrowExceptionIfNameIsNull() throws CloudException, InternalException {
        loadBalancerSupport.createLoadBalancer(LoadBalancerCreateOptions.getInstance(null, LB_DESCRIPTION));
    }

    @Test(expected = InternalException.class)
    public void createLoadBalancerShouldThrowExceptionIfNameIsEmpty() throws CloudException, InternalException {
        loadBalancerSupport.createLoadBalancer(LoadBalancerCreateOptions.getInstance("", LB_DESCRIPTION));
    }

    @Test(expected = InternalException.class)
    public void createLoadBalancerShouldThrowExceptionIfHCProtocolIsTCP() throws CloudException, InternalException {
        HealthCheckOptions healthCheckOptions = HealthCheckOptions.getInstance(LB_NAME, HC_DESCRIPTION, null, null,
                LoadBalancerHealthCheck.HCProtocol.TCP, HC_PORT, HC_PATH, 9, 9, 9, 9);
        LoadBalancerCreateOptions loadBalancerCreateOptions = LoadBalancerCreateOptions.getInstance(LB_NAME,
                LB_DESCRIPTION);
        loadBalancerCreateOptions.withHealthCheckOptions(healthCheckOptions);
        loadBalancerSupport.createLoadBalancer(loadBalancerCreateOptions);
    }

    @Test(expected = InternalException.class)
    public void createLoadBalancerShouldThrowExceptionIfHCProtocolIsSSL() throws CloudException, InternalException {
        HealthCheckOptions healthCheckOptions = HealthCheckOptions.getInstance(LB_NAME, HC_DESCRIPTION, null, null,
                LoadBalancerHealthCheck.HCProtocol.SSL, HC_PORT, HC_PATH, 9, 9, 9, 9);
        LoadBalancerCreateOptions loadBalancerCreateOptions = LoadBalancerCreateOptions.getInstance(LB_NAME,
                LB_DESCRIPTION);
        loadBalancerCreateOptions.withHealthCheckOptions(healthCheckOptions);
        loadBalancerSupport.createLoadBalancer(loadBalancerCreateOptions);
    }

    @Test(expected = AzureException.class)
    public void createLoadBalancerShouldThrowExceptionIfNameIsExist() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("POST".equals(request.getMethod()) && PROFILES_URL.equals(request.getURI().toString())) {
                    assertPost(request, PROFILES_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            createProfileModel());

                    AzureOperationStatus.AzureOperationError error = new AzureOperationStatus.AzureOperationError();
                    error.setCode("BadRequest");
                    error.setMessage("A conflict occurred to prevent the operation from completing.");
                    DaseinObjectToXmlEntity<AzureOperationStatus.AzureOperationError> daseinEntity = new DaseinObjectToXmlEntity<AzureOperationStatus.AzureOperationError>(
                            error);

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_BAD_REQUEST), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }

            }
        };

        HealthCheckOptions healthCheckOptions = HealthCheckOptions.getInstance(LB_NAME, HC_DESCRIPTION, null, null,
                HC_PROTOCOL, HC_PORT, HC_PATH, 9, 9, 9, 9);
        LoadBalancerCreateOptions loadBalancerCreateOptions = LoadBalancerCreateOptions.getInstance(LB_NAME,
                LB_DESCRIPTION);
        loadBalancerCreateOptions.withHealthCheckOptions(healthCheckOptions);
        loadBalancerSupport.createLoadBalancer(loadBalancerCreateOptions);
    }

    @Test
    public void createLoadBalancerShouldThrowPostCorrectRequestIfLBListenersIsNull() throws CloudException, InternalException {
        CreateLoadBalancerMockUp mockUp = new CreateLoadBalancerMockUp("RoundRobin");
        LoadBalancerCreateOptions loadBalancerCreateOptions = createLoadBalancerCreateOptions(null);
        String result = loadBalancerSupport.createLoadBalancer(loadBalancerCreateOptions);
        assertEquals("LoadBalancerSupport.createLoadBalancer() doesn't return correct result", LB_NAME, result);
        assertEquals("Post profiles count doesn't match", 1, mockUp.postProfilesCount);
        assertEquals("Post definitions count doesn't match", 1, mockUp.postDefinitionsCount);
    }

    @Test
    public void createLoadBalancerShouldPostCorrectRequestIfLbAlgorithmIsSOURCE() throws CloudException, InternalException {
        CreateLoadBalancerMockUp mockUp = new CreateLoadBalancerMockUp("Performance");
        String result = loadBalancerSupport.createLoadBalancer(createLoadBalancerCreateOptions(LbAlgorithm.SOURCE));
        assertEquals("LoadBalancerSupport.createLoadBalancer() doesn't return correct result", LB_NAME, result);
        assertEquals("Post profiles count doesn't match", 1, mockUp.postProfilesCount);
        assertEquals("Post definitions count doesn't match", 1, mockUp.postDefinitionsCount);
    }

    @Test
    public void removeLoadBalancerShouldDeleteCorrectRequest() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("DELETE".equals(request.getMethod())) {
                    assertDelete(request, PROFILE_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        loadBalancerSupport.removeLoadBalancer(LB_NAME);
    }

    @Test
    public void createLoadBalancerShouldPostCorrectRequestIfLbAlgorithmIsLEAST_CONN() throws CloudException, InternalException {
        CreateLoadBalancerMockUp mockUp = new CreateLoadBalancerMockUp("Failover");
        String result = loadBalancerSupport.createLoadBalancer(createLoadBalancerCreateOptions(LbAlgorithm.LEAST_CONN));
        assertEquals("LoadBalancerSupport.createLoadBalancer() doesn't return correct result", LB_NAME, result);
        assertEquals("Post profiles count doesn't match", 1, mockUp.postProfilesCount);
        assertEquals("Post definitions count doesn't match", 1, mockUp.postDefinitionsCount);
    }

    @Test
    public void createLoadBalancerShouldPostCorrectRequestIfLbAlgorithmIsROUND_ROBIN() throws CloudException, InternalException {
        CreateLoadBalancerMockUp mockUp = new CreateLoadBalancerMockUp("RoundRobin");
        String result = loadBalancerSupport.createLoadBalancer(createLoadBalancerCreateOptions(LbAlgorithm.ROUND_ROBIN));
        assertEquals("LoadBalancerSupport.createLoadBalancer() doesn't return correct result", LB_NAME, result);
        assertEquals("Post profiles count doesn't match", 1, mockUp.postProfilesCount);
        assertEquals("Post definitions count doesn't match", 1, mockUp.postDefinitionsCount);
    }

    @Test
    public void listLoadBalancersShouldReturnCorrectResult() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod())  && PROFILES_URL.equals(request.getURI().toString())) {
                    assertGet(request, PROFILES_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DaseinObjectToXmlEntity<ProfilesModel> daseinEntity = new DaseinObjectToXmlEntity<ProfilesModel>(
                            createProfilesModel());

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        List<LoadBalancer> loadBalancers = IteratorUtils.toList(loadBalancerSupport.listLoadBalancers().iterator());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result size", 1,
                loadBalancers.size());
        LoadBalancer loadBalancer = loadBalancers.get(0);
        assertLoadBalancer(loadBalancer);
    }

    @Test
    public void getLoadBalancerShouldReturnNullIfIsNotExist() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod())  && PROFILE_URL.equals(request.getURI().toString())) {
                    assertGet(request, PROFILE_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        assertNull("", loadBalancerSupport.getLoadBalancer(LB_NAME));
    }

    @Test
    public void getLoadBalancerShouldReturnCorrectResult() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod())  && PROFILE_URL.equals(request.getURI().toString())) {
                    assertGet(request, PROFILE_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DaseinObjectToXmlEntity<ProfileModel> daseinEntity = new DaseinObjectToXmlEntity<ProfileModel>(
                            createProfileModel());

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        LoadBalancer loadBalancer = loadBalancerSupport.getLoadBalancer(LB_NAME);
        assertLoadBalancer(loadBalancer);
    }

    @Test
    public void addServersShouldPostCorrectRequest() throws CloudException, InternalException {
        final String ROLE_NAME_2 = "TESTROLENAME2";
        final String VM_ID_2 = String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME_2);

        final AtomicInteger postCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if ("POST".equals(request.getMethod()) && DEFINITIONS_URL.equals(request.getURI().toString())) {
                    postCount.incrementAndGet();
                    assertPost(request, DEFINITIONS_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            createDefinitionModelWithAnotherServer("Failover", "Enabled", ROLE_NAME_2));

                    DefinitionModel definitionModel = new DefinitionModel();
                    definitionModel.setVersion("2");
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            definitionModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                }else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        loadBalancerSupport.addServers(LB_NAME, ROLE_NAME_2);
        assertEquals("LoadBalancerSupport.addServers() ", 1, postCount.get());
    }

    @Test
    public void removeServersShouldPostCorrectRequest() throws CloudException, InternalException {
        final String ROLE_NAME_2 = "TESTROLENAME2";
        final String VM_ID_2 = String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME_2);

        final AtomicInteger postCount = new AtomicInteger(0);
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });
                    DefinitionModel definitionModel = createDefinitionModelWithAnotherServer("Failover", "Enabled",
                            ROLE_NAME_2);

                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if ("POST".equals(request.getMethod()) && DEFINITIONS_URL.equals(request.getURI().toString())) {
                    postCount.incrementAndGet();
                    assertPost(request, DEFINITIONS_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            createDefinitionModel("Failover", "Enabled", HC_PORT));

                    DefinitionModel definitionModel = new DefinitionModel();
                    definitionModel.setVersion("2");
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            definitionModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        loadBalancerSupport.removeServers(LB_NAME, ROLE_NAME_2);
        assertEquals("LoadBalancerSupport.addServers() post count doesn't match", 1, postCount.get());
    }

    @Test
    public void listEndpointsShouldReturnCorrectResult(@Mocked final AzureVM azureVM) throws CloudException, InternalException {
        new NonStrictExpectations() {{
            VirtualMachine virtualMachine = new VirtualMachine();
            virtualMachine.setPublicDnsAddress(String.format("%s.cloudapp.net", ROLE_NAME));
            virtualMachine.setProviderVirtualMachineId(VM_ID);
            List<VirtualMachine> virtualMachines = new ArrayList<VirtualMachine>();
            virtualMachines.add(virtualMachine);
            azureVM.listVirtualMachines(); result = virtualMachines;
        }};
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        assertLoadBalancerEndpoints(IteratorUtils.toList(loadBalancerSupport.listEndpoints(LB_NAME).iterator()));
        //another round to test cache
        assertLoadBalancerEndpoints(IteratorUtils.toList(loadBalancerSupport.listEndpoints(LB_NAME).iterator()));
    }

    @Test
    public void listLBHealthChecksShouldReturnEmptyIfProfilesIsNotFound() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && PROFILES_URL.equals(request.getURI().toString())) {
                    assertGet(request, PROFILES_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_NOT_FOUND), null,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        List<LoadBalancerHealthCheck> loadBalancerHealthChecks = IteratorUtils.toList(loadBalancerSupport
                .listLBHealthChecks(HealthCheckFilterOptions.getInstance(true)
                        .matchingProtocol(LoadBalancerHealthCheck.HCProtocol.HTTP)).iterator());
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return size doesn't match", 0,
                loadBalancerHealthChecks.size());
    }

    @Test
    public void listLBHealthChecksShouldReturnCorrectResult() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && PROFILES_URL.equals(request.getURI().toString())) {
                    assertGet(request, PROFILES_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    DaseinObjectToXmlEntity<ProfilesModel> daseinEntity = new DaseinObjectToXmlEntity<ProfilesModel>(
                            createProfilesModel());
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        List<LoadBalancerHealthCheck> loadBalancerHealthChecks = IteratorUtils.toList(loadBalancerSupport
                .listLBHealthChecks(HealthCheckFilterOptions.getInstance(true)
                        .matchingProtocol(LoadBalancerHealthCheck.HCProtocol.HTTP)).iterator());
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return size doesn't match", 1,
                loadBalancerHealthChecks.size());
        LoadBalancerHealthCheck loadBalancerHealthCheck = loadBalancerHealthChecks.get(0);
        assertLoadBalancerHealthCheck(loadBalancerHealthCheck, HC_PORT);
    }

    @Test(expected = InternalException.class)
    public void getLoadBalancerHealthCheckShouldThrowExceptionIfProviderLBHealthCheckIdIsNull() throws CloudException, InternalException {
        loadBalancerSupport.getLoadBalancerHealthCheck(null, LB_NAME);
    }

    @Test
    public void getLoadBalancerHealthCheckShouldReturnCorrectResult() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            createDefinitionModel("Failover", "Enabled", HC_PORT));
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };
        assertLoadBalancerHealthCheck(loadBalancerSupport.getLoadBalancerHealthCheck(LB_NAME, LB_NAME), HC_PORT);
        assertLoadBalancerHealthCheck(loadBalancerSupport.getLoadBalancerHealthCheck(LB_NAME, null), HC_PORT);
    }

    @Test(expected = InternalException.class)
    public void modifyHealthCheckShouldThrowExceptionIfLoadBalancerIdIsNull() throws CloudException, InternalException {
        loadBalancerSupport.modifyHealthCheck(null, HealthCheckOptions
                .getInstance(LB_NAME, HC_DESCRIPTION, null, null, HC_PROTOCOL, HC_PORT, HC_PATH, 9, 9, 9, 9));
    }

    @Test(expected = InternalException.class)
    public void modifyHealthCheckShouldThrowExceptionIfHCProtocolIsTCP() throws CloudException, InternalException {
        loadBalancerSupport.modifyHealthCheck(LB_NAME, HealthCheckOptions
                .getInstance(LB_NAME, HC_DESCRIPTION, LB_NAME, null, LoadBalancerHealthCheck.HCProtocol.TCP, HC_PORT,
                        HC_PATH, 9, 9, 9, 9));
    }

    @Test(expected = InternalException.class)
    public void modifyHealthCheckShouldThrowExceptionIfHCProtocolIsSSL() throws CloudException, InternalException {
        loadBalancerSupport.modifyHealthCheck(LB_NAME, HealthCheckOptions
                .getInstance(LB_NAME, HC_DESCRIPTION, LB_NAME, null, LoadBalancerHealthCheck.HCProtocol.SSL, HC_PORT,
                        HC_PATH, 9, 9, 9, 9));
    }

    @Test
    public void modifyHealthCheckShouldPostCorrectRequest() throws CloudException, InternalException {
        final int portChangeTo = 8080;
        final AtomicInteger getCount = new AtomicInteger(0);
        final AtomicInteger postCount = new AtomicInteger(0);

        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if ("GET".equals(request.getMethod()) && DEFINITION_URL.equals(request.getURI().toString())) {
                    getCount.incrementAndGet();
                    assertGet(request, DEFINITION_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") });

                    if(getCount.get() == 1) {
                        DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                                createDefinitionModel("Failover", "Enabled", HC_PORT));
                        return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity, new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                    } else {
                        DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                                createDefinitionModel("Failover", "Enabled", portChangeTo));
                        return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity, new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                    }
                }  else if ("POST".equals(request.getMethod()) && DEFINITIONS_URL.equals(request.getURI().toString())) {
                    postCount.incrementAndGet();
                    assertPost(request, DEFINITIONS_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                            createDefinitionModel("Failover", "Enabled", portChangeTo));

                    DefinitionModel definitionModel = new DefinitionModel();
                    definitionModel.setVersion("2");
                    DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                            definitionModel);
                    return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                            new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
                } else {
                    throw new IOException("Request is not mocked");
                }
            }
        };

        LoadBalancerHealthCheck loadBalancerHealthCheck = loadBalancerSupport.modifyHealthCheck(LB_NAME,
                HealthCheckOptions.getInstance(LB_NAME, HC_DESCRIPTION, LB_NAME, null, HC_PROTOCOL, 8080, HC_PATH, 9, 9, 9, 9));
        assertEquals("LoadBalancerSupport.modifyHealthCheck() post count doesn't match", 1, postCount.get());
        assertLoadBalancerHealthCheck(loadBalancerHealthCheck, portChangeTo);
    }

    private void assertLoadBalancerHealthCheck(LoadBalancerHealthCheck loadBalancerHealthCheck, int port) {
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return doesn't match", LB_NAME, loadBalancerHealthCheck.getProviderLBHealthCheckId());
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return doesn't match", HC_PATH, loadBalancerHealthCheck.getPath());
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return doesn't match", port, loadBalancerHealthCheck.getPort());
        assertEquals("LoadBalancerSupport.listLBHealthChecks() return doesn't match", LoadBalancerHealthCheck.HCProtocol.HTTP, loadBalancerHealthCheck.getProtocol());
    }

    private void assertLoadBalancerEndpoints(List<LoadBalancerEndpoint> endpoints) {
        assertEquals("LoadBalancerSupport.listEndpoints() return size doesn't match", 1, endpoints.size());
        LoadBalancerEndpoint endpoint = endpoints.get(0);
        assertEquals("LoadBalancerSupport.listEndpoints() return doesn't match", LbEndpointState.ACTIVE, endpoint.getCurrentState());
        assertEquals("LoadBalancerSupport.listEndpoints() return doesn't match", VM_ID, endpoint.getEndpointValue());
        assertEquals("LoadBalancerSupport.listEndpoints() return doesn't match", LbEndpointType.VM, endpoint.getEndpointType());
    }

    private void assertLoadBalancer(LoadBalancer loadBalancer) {
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LB_DOMAIN,
                loadBalancer.getAddress());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result",
                LoadBalancerAddressType.DNS, loadBalancer.getAddressType());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LoadBalancerState.ACTIVE,
                loadBalancer.getCurrentState());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LB_NAME,
                loadBalancer.getName());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LB_NAME,
                loadBalancer.getProviderLoadBalancerId());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", ACCOUNT_NO,
                loadBalancer.getProviderOwnerId());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result",
                new IPVersion[] { IPVersion.IPV4 }, loadBalancer.getSupportedTraffic());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LB_NAME,
                loadBalancer.getProviderLBHealthCheckId());

        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", 1,
                loadBalancer.getListeners().length);
        LbListener lbListener = loadBalancer.getListeners()[0];
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LbAlgorithm.LEAST_CONN,
                lbListener.getAlgorithm());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", LbPersistence.COOKIE,
                lbListener.getPersistence());
        assertEquals("LoadBalancerSupport.listLoadBalancers() doesn't return correct result", "",
                lbListener.getCookie());
    }

    private LoadBalancerCreateOptions createLoadBalancerCreateOptions(LbAlgorithm algorithm) {
        LoadBalancerCreateOptions loadBalancerCreateOptions = LoadBalancerCreateOptions.getInstance(LB_NAME, LB_DESCRIPTION);
        HealthCheckOptions healthCheckOptions = HealthCheckOptions.getInstance(LB_NAME, HC_DESCRIPTION, null, null,
                HC_PROTOCOL, HC_PORT, HC_PATH, 9, 9, 9, 9);
        loadBalancerCreateOptions.withHealthCheckOptions(healthCheckOptions);
        if (algorithm != null) {
            LbListener lbListener = LbListener
                    .getInstance(algorithm, "jsessionid", LB_PROTOCOL, LB_PUBLIC_PORT, LB_PRIVATE_PORT);
            loadBalancerCreateOptions.havingListeners(lbListener);
        }
        loadBalancerCreateOptions.withVirtualMachines(VM_ID);
        return loadBalancerCreateOptions;
    }

    private DefinitionModel createDefinitionModelWithAnotherServer(String loadBalancingMethod, String status, String anotherRole) {
        DefinitionModel definitionModel = createDefinitionModel(loadBalancingMethod, status, HC_PORT);
        DefinitionModel.EndPointModel endPointModel = new DefinitionModel.EndPointModel();
        endPointModel.setDomainName(anotherRole + ".cloudapp.net");
        endPointModel.setStatus("Enabled");
        endPointModel.setType("CloudService");

        definitionModel.getPolicy().getEndPoints().add(endPointModel);
        return definitionModel;
    }

    private DefinitionModel createDefinitionModel(String loadBalancingMethod, String status, int port) {
        DefinitionModel definition = new DefinitionModel();

        DefinitionModel.DnsOptions dnsOptions = new DefinitionModel.DnsOptions();
        dnsOptions.setTimeToLiveInSeconds("300");
        definition.setDnsOptions(dnsOptions);

        DefinitionModel.MonitorModel monitor = new DefinitionModel.MonitorModel();
        monitor.setIntervalInSeconds("30");
        monitor.setTimeoutInSeconds("10");
        monitor.setToleratedNumberOfFailures("3");
        monitor.setProtocol(HC_PROTOCOL.toString());
        monitor.setPort(String.valueOf(port));
        DefinitionModel.HttpOptionsModel httpOptions = new DefinitionModel.HttpOptionsModel();
        httpOptions.setVerb("GET");
        httpOptions.setRelativePath(HC_PATH);
        httpOptions.setExpectedStatusCode("200");
        monitor.setHttpOptions(httpOptions);

        ArrayList<DefinitionModel.MonitorModel> monitors = new ArrayList<DefinitionModel.MonitorModel>();
        monitors.add(monitor);
        definition.setMonitors(monitors);

        DefinitionModel.PolicyModel policy = new DefinitionModel.PolicyModel();
        policy.setLoadBalancingMethod(loadBalancingMethod);

        ArrayList<DefinitionModel.EndPointModel> endPointsToAdd = new ArrayList<DefinitionModel.EndPointModel>();
        DefinitionModel.EndPointModel endPointModel = new DefinitionModel.EndPointModel();
        endPointModel.setDomainName(String.format("%s.cloudapp.net", ROLE_NAME));
        endPointModel.setStatus("Enabled");
        endPointModel.setType("CloudService");
        endPointsToAdd.add(endPointModel);

        policy.setEndPoints(endPointsToAdd);
        definition.setPolicy(policy);

        definition.setStatus(status);

        return definition;
    }

    private ProfilesModel createProfilesModel() {
        ProfilesModel profilesModel = new ProfilesModel();
        List<ProfileModel> profileModels = new ArrayList<ProfileModel>();
        profileModels.add(createProfileModel());
        profilesModel.setProfiles(profileModels);
        return profilesModel;
    }

    private ProfileModel createProfileModel() {
        ProfileModel profileModel = new ProfileModel();
        profileModel.setDomainName(LB_DOMAIN);
        profileModel.setName(LB_NAME);
        return profileModel;
    }

    private class CreateLoadBalancerMockUp extends MockUp<CloseableHttpClient> {
        private String loadBalancingMethod;
        private int postProfilesCount = 0;
        private int postDefinitionsCount = 0;

        private CreateLoadBalancerMockUp(String loadBalancingMethod) {
            this.loadBalancingMethod = loadBalancingMethod;
        }
        @Mock
        public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
            if ("POST".equals(request.getMethod()) && PROFILES_URL.equals(request.getURI().toString())) {
                postProfilesCount++;
                assertPost(request, PROFILES_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                        createProfileModel());

                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else if ("POST".equals(request.getMethod()) && DEFINITIONS_URL.equals(request.getURI().toString())) {
                postDefinitionsCount++;
                assertPost(request, DEFINITIONS_URL, new Header[] { new BasicHeader("x-ms-version", "2012-03-01") },
                        createDefinitionModel(loadBalancingMethod, null, HC_PORT));

                DefinitionModel definitionModel = new DefinitionModel();
                definitionModel.setVersion("1");
                DaseinObjectToXmlEntity<DefinitionModel> daseinEntity = new DaseinObjectToXmlEntity<DefinitionModel>(
                        definitionModel);
                return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity,
                        new Header[] { new BasicHeader("x-ms-request-id", UUID.randomUUID().toString()) });
            } else {
                throw new IOException("Request is not mocked");
            }
        }
    }
}
