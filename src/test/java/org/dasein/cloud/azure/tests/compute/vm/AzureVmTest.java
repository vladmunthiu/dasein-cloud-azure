/**
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azure.tests.compute.vm;

import mockit.*;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.compute.AzureAffinityGroupSupport;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.compute.vm.model.HostedServiceModel;
import org.dasein.cloud.azure.compute.vm.model.Operation;
import org.dasein.cloud.azure.network.AzureNetworkServices;
import org.dasein.cloud.azure.network.AzureVlanSupport;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Created by Vlad_Munthiu on 6/6/2014.
 */
public class AzureVmTest extends AzureTestsBase {

    private static String ROLE_OPERATIONS_URL = "%s/%s/services/hostedservices/%s/deployments/%s/roleInstances/%s/Operations";
    private static String ROLE_URL = "%s/%s/services/hostedservices/%s/deployments/%s/roles/%s";
    private static String HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL = "%s/%s/services/hostedservices/%s?embed-detail=true";

    @Mocked AzureComputeServices computeServiceMock;
    @Mocked AzureAffinityGroupSupport affinitySupportMock;
    @Mocked AzureLocation dataCenterServiceMock;

    final String REGION_NAME = "TEST_REGION_NAME";

    private VirtualMachine getTestVirtualMachine() {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.addTag("serviceName", SERVICE_NAME);
        virtualMachine.addTag("deploymentName", DEPLOYMENT_NAME);
        virtualMachine.addTag("roleName", ROLE_NAME);
        virtualMachine.setProviderVirtualMachineId(VM_ID);
        return virtualMachine;
    }

    @Test
    public void startShouldPostCorrectRequest()throws CloudException, InternalException{
        final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
        final String expectedUrl = String.format(ROLE_OPERATIONS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
                assertPost(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-02-01")}, new Operation.StartRoleOperation());
                return mockedHttpResponse;
            }
        };

        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, getTestVirtualMachine());
        azureVMSupport.start(VM_ID);
    }

    @Test(expected = InternalException.class)
    public void startShouldThrowExceptionIfVmIdIsNull() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        azureVMSupport.start(null);
    }

    @Test(expected = CloudException.class)
    public void startShouldThrowExceptionIfNoVmForPassedInVmId() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, null);
        azureVMSupport.start(VM_ID);
    }

    @Test(expected = InternalException.class)
    public void startShouldThrowExceptionIfDeserializationFails() throws CloudException, InternalException {
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws JAXBException {
                throw new JAXBException("Deserialization failed");
            }
        };

        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, getTestVirtualMachine());
        azureVMSupport.start(VM_ID);
    }

    @Test(expected = AzureConfigException.class)
    public void alterVirtualMachineProductShouldThrowExceptionIfVMIdNull() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        azureVMSupport.alterVirtualMachineProduct(null, "something");
    }

    @Test(expected = AzureConfigException.class)
    public void alterVirtualMachineProductShouldThrowExceptionIfProductIdNull() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        azureVMSupport.alterVirtualMachineProduct("something", null);
    }

    @Test(expected = InternalException.class)
    public void alterVirtualMachineProductShouldThrowExceptionIfProductIdNotValid() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        azureVMSupport.alterVirtualMachineProduct("something", "TESTPRODUCT");
    }

    @Test
    public void testAlterVirtualMachineProduct() throws CloudException, InternalException {
        final PersistentVMRoleModel persistentVMRoleModel = getPersistentVMRoleModel();

        DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(persistentVMRoleModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity , new Header[]{});
        final CloseableHttpResponse putHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format(ROLE_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

        final VirtualMachine virtualMachine = getTestVirtualMachine();
        final AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, virtualMachine);

        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if(inv.getInvocationCount() == 1) {
                    assertGet(request, expectedUrl);
                    return getHttpResponseMock;
                } else {
                    persistentVMRoleModel.setRoleSize("Small");
                    assertPut(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2012-03-01")}, persistentVMRoleModel);
                    virtualMachine.setProductId("Small");
                    azureVMSupport.setVirtualMachine(virtualMachine);
                    return putHttpResponseMock;
                }
            }
        };

        VirtualMachine actualReturnedVirtualMachine = azureVMSupport.alterVirtualMachineProduct(VM_ID, "Small");
        assertReflectionEquals("Alter method does not return the correct virtual machine", virtualMachine, actualReturnedVirtualMachine);
    }

    @Test
    public void testAlterMethodDoesNotPostDataWhenVMAlreadyHasRequiredProducts() throws CloudException, InternalException {
        final PersistentVMRoleModel persistentVMRoleModel = getPersistentVMRoleModel();
        persistentVMRoleModel.setRoleSize("Small");

        DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(persistentVMRoleModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity , new Header[]{});
        final String expectedUrl = String.format(ROLE_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

        final VirtualMachine virtualMachine = getTestVirtualMachine();
        virtualMachine.setProductId("Small");
        final AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, virtualMachine);

        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                    assertGet(request, expectedUrl);
                    return getHttpResponseMock;
            }
        };

        VirtualMachine actualReturnedVirtualMachine = azureVMSupport.alterVirtualMachineProduct(VM_ID, "Small");
        assertReflectionEquals("Alter method does not return the correct virtual machine", virtualMachine, actualReturnedVirtualMachine);
    }

    //TODO test alter does wait for operation status

    @Test(expected = OperationNotSupportedException.class)
    public void testCloneNotSupported() throws CloudException, InternalException {
        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        azureVMSupport.clone("EXPECTED","EXPECTED", "EXPECTED", "EXPECTED", false, "EXPECTED");
    }

    @Test
    public void testGetProductReturnsNullWhenThereAreNoProducts() throws CloudException, InternalException {
        final AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);
        new Expectations(AzureVMSupport.class){
            {azureVMSupport.listProducts(null, null); result = new ArrayList<VirtualMachineProduct>();}
        };

        Assert.assertNull(azureVMSupport.getProduct("ANY_PRODUCT"));
    }

    @Test
    public void testGetProductReturnsNullWhenProductNotInTheList() throws CloudException, InternalException {
        final AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);

        final ArrayList<VirtualMachineProduct> products = new ArrayList<VirtualMachineProduct>();
        for(final String providerId : Arrays.asList("FISRT", "SECOND", "THIRD")) {
            products.add(getTestVirtualMachineProducts(providerId));
        }

        new Expectations(AzureVMSupport.class){
            {azureVMSupport.listProducts(null, null); result = products;}
        };

        Assert.assertNull(azureVMSupport.getProduct("ANY_PRODUCT"));
    }

    @Test
    public void testGetProductReturnsCorrectProduct() throws CloudException, InternalException {
        final AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock);

        final ArrayList<VirtualMachineProduct> products = new ArrayList<VirtualMachineProduct>();
        for(final String providerId : Arrays.asList("FISRT", "SECOND", "THIRD")) {
            products.add(getTestVirtualMachineProducts(providerId));
        }

        VirtualMachineProduct expectedProduct = getTestVirtualMachineProducts("ANY_PRODUCT");
        products.add(expectedProduct);

        new Expectations(AzureVMSupport.class){
            {azureVMSupport.listProducts(null, null); result = products;}
        };

        VirtualMachineProduct actualProduct = azureVMSupport.getProduct("ANY_PRODUCT");
        Assert.assertNotNull(actualProduct);
        assertReflectionEquals(expectedProduct, actualProduct);

    }

    private VirtualMachineProduct getTestVirtualMachineProducts(String productId) {
        VirtualMachineProduct prd = new VirtualMachineProduct();
        prd.setProviderProductId(productId);
        prd.setName("PRODUCT_NAME");
        prd.setDescription("DESCRIPTION_NAME");
        prd.setCpuCount(100);
        prd.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
        prd.setRamSize(new Storage<Megabyte>(512, Storage.MEGABYTE));
        prd.setStandardHourlyRate((float)50.5);
        return prd;
    }

    private PersistentVMRoleModel getPersistentVMRoleModel() {
        final PersistentVMRoleModel persistentVMRoleModel = new PersistentVMRoleModel();
        persistentVMRoleModel.setRoleName(VM_NAME);
        persistentVMRoleModel.setOsVersion("EXPECTED_OSVERSION");
        persistentVMRoleModel.setRoleType("EXPECTED_ROLETYPES");
        persistentVMRoleModel.setRoleSize("SIZE_TO_BE_CHANGE");
        PersistentVMRoleModel.OSVirtualHardDisk osVirtualHardDisk = new PersistentVMRoleModel.OSVirtualHardDisk();
        osVirtualHardDisk.setDiskLabel("EXPECTED_DISKLABEL");
        osVirtualHardDisk.setHostCaching("EXPECTED_HOSTCACHING");
        osVirtualHardDisk.setMediaLink("EXPECTED_MEDIALINK");
        osVirtualHardDisk.setSourceImageName("EXPECTED_IMAGENAME");
        persistentVMRoleModel.setOsVirtualHardDisk(osVirtualHardDisk);
        PersistentVMRoleModel.ConfigurationSet configurationSet = new PersistentVMRoleModel.ConfigurationSet();
        configurationSet.setConfigurationSetType("EXPECTED_CONFIGSET");
        PersistentVMRoleModel.InputEndpoint inputEndpoint = new PersistentVMRoleModel.InputEndpoint();
        inputEndpoint.setName("EXPECTED_ENPOINT_NAME");
        inputEndpoint.setLocalPort("EXPECTED_LOCAL_PORT");
        inputEndpoint.setPort("EXPECTED_PORT");
        inputEndpoint.setProtocol("EXPECTED_PROTOCOL");
        configurationSet.setInputEndpoints(Arrays.asList(inputEndpoint));
        persistentVMRoleModel.setConfigurationSets(Arrays.asList(configurationSet));
        return persistentVMRoleModel;
    }

    @Test(expected = InternalException.class)
    public void testGetVirtualMachineThrowsExceptionWhenVmIdNull() throws CloudException, InternalException {
        AzureVM azureVM = new AzureVM(azureMock);
        azureVM.getVirtualMachine(null);
    }

    @Test
    public void testGetVirtualMachineGetProperServiceNameFromIdWith3Parts() throws CloudException, InternalException {

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl);
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVirtualMachineGetProperServiceNameFromIdWith2Parts() throws CloudException, InternalException {

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl);
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s", SERVICE_NAME, DEPLOYMENT_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVirtualMachineGetProperServiceNameFromIdWith1Part() throws CloudException, InternalException {

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl);
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(SERVICE_NAME);
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMIncorrectLocationReturnsNullVM() throws CloudException, InternalException {
        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setLocation("WRONG_LOCATION");
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);

        DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMNullAGReturnsNullVM() throws CloudException, InternalException {
        String affinityGroupId = "TEST_AFFINITY_ID";

        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setAffinityGroup(affinityGroupId);
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);

        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { affinitySupportMock.get(anyString); result = null;}
        };

        DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMIncorrectAGReturnsNullVM() throws CloudException, InternalException {
        String affinityGroupId = "TEST_AFFINITY_ID";

        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setAffinityGroup(affinityGroupId);
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);

        final AffinityGroup testAffinityGroup = AffinityGroup.getInstance(affinityGroupId,"AG_NAME", "AG_DESCRIPTION", "WRONG_DATACENTER", null);
        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { affinitySupportMock.get(anyString); result = testAffinityGroup;}
            { azureMock.getDataCenterServices(); result = dataCenterServiceMock; }
            { dataCenterServiceMock.getDataCenter("WRONG_DATACENTER"); result = new DataCenter(REGION, REGION_NAME, "WRONG_REGION", true, true);}
        };

        final DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMNoDeploymentsReturnNull() throws CloudException, InternalException {
        String affinityGroupId = "TEST_AFFINITY_ID";

        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setAffinityGroup(affinityGroupId);
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);

        final AffinityGroup testAffinityGroup = AffinityGroup.getInstance(affinityGroupId,"AG_NAME", "AG_DESCRIPTION", REGION, null);
        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { affinitySupportMock.get(anyString); result = testAffinityGroup;}
            { azureMock.getDataCenterServices(); result = dataCenterServiceMock; }
            { dataCenterServiceMock.getDataCenter(anyString); result = new DataCenter(REGION, REGION_NAME, REGION, true, true);}
        };

        final DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMWrongDeploymentNameReturnNull() throws CloudException, InternalException {
        String affinityGroupId = "TEST_AFFINITY_ID";

        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setAffinityGroup(affinityGroupId);
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);
        HostedServiceModel.Deployment deployment = new HostedServiceModel.Deployment();
        deployment.setName("WRONG_DEPLOYMENT_NAME");
        hostedServiceModel.setDeployments(Arrays.asList(deployment));

        final AffinityGroup testAffinityGroup = AffinityGroup.getInstance(affinityGroupId,"AG_NAME", "AG_DESCRIPTION", REGION, null);
        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { affinitySupportMock.get(anyString); result = testAffinityGroup;}
            { azureMock.getDataCenterServices(); result = dataCenterServiceMock; }
            { dataCenterServiceMock.getDataCenter(anyString); result = new DataCenter(REGION, REGION_NAME, REGION, true, true);}
        };

        final DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        Assert.assertNull(virtualMachine);
    }

    @Test
    public void testGetVMReturnsCorrectVM(@Mocked final AzureOSImage imageSupportMock, @Mocked final AzureNetworkServices networkServicesMock, @Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, URISyntaxException {
        String affinityGroupId = "TEST_AFFINITY_ID";

        HostedServiceModel hostedServiceModel = getHostedServiceModel(affinityGroupId);

        final MachineImage testMachineImage = MachineImage.getInstance(ACCOUNT_NO,REGION,"DISK_SOURCE_IMAGE_NAME", ImageClass.MACHINE,MachineImageState.ACTIVE,"DISK_SOURCE_IMAGE_NAME","DISK_SOURCE_IMAGE_NAME",Architecture.I64,Platform.WINDOWS);
        final DataCenter testDataCenter = new DataCenter(REGION, REGION_NAME, REGION, true, true);
        final AffinityGroup testAffinityGroup = AffinityGroup.getInstance(affinityGroupId,"AG_NAME", "AG_DESCRIPTION", REGION, null);
        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { computeServiceMock.getImageSupport(); result = imageSupportMock; }
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
            { imageSupportMock.getMachineImage("DISK_SOURCE_IMAGE_NAME"); result = testMachineImage;}
            { affinitySupportMock.get(anyString); result = testAffinityGroup;}
            { azureMock.getDataCenterServices(); result = dataCenterServiceMock; }
            { dataCenterServiceMock.getDataCenter(anyString); result = testDataCenter;}
        };

        final DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);

        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICES_SERVICE_EMBEDED_DETAILS_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME);
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                assertGet(request, expectedUrl, new Header[]{new BasicHeader("x-ms-version", "2014-05-01")});
                return getHttpResponseMock;
            }
        };

        AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.getVirtualMachine(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME));
        assertVirtualMachine(hostedServiceModel, testMachineImage, virtualMachine);
    }

    private HostedServiceModel getHostedServiceModel(String affinityGroupId) {
        HostedServiceModel hostedServiceModel = new HostedServiceModel();
        HostedServiceModel.HostedServiceProperties hostedServiceProperties = new HostedServiceModel.HostedServiceProperties();
        hostedServiceProperties.setAffinityGroup(affinityGroupId);
        hostedServiceModel.setHostedServiceProperties(hostedServiceProperties);
        HostedServiceModel.Deployment deployment = new HostedServiceModel.Deployment();
        deployment.setName(DEPLOYMENT_NAME);
        deployment.setPrivateId("DEPLOYMENT_PRIVATE_ID");
        deployment.setLocked("true");
        deployment.setUrl("http://test.com");
        deployment.setDeploymentSlot("DEPLOYMENT_SLOT");

        HostedServiceModel.RoleInstance roleInstance = new HostedServiceModel.RoleInstance();
        roleInstance.setRoleName(VM_NAME);
        roleInstance.setInstanceSize("ROLE_INSTANCE_SIZE");
        roleInstance.setInstanceUpgradeDomain("ROLE_UPGRADE_DOMAIN");
        roleInstance.setInstanceErrorCode("ROLE_INSTANCE_ERROR_CODE");
        roleInstance.setInstanceFaultDomain("ROLE_INSTANCE_FAULT_DOMAIN");
        roleInstance.setIpAddress("199.199.199.199");
        HostedServiceModel.InstanceEndpoint instanceEndpoint = new HostedServiceModel.InstanceEndpoint();
        instanceEndpoint.setVip("201.201.201.201");
        roleInstance.setInstanceEndpoints(Arrays.asList(instanceEndpoint));
        roleInstance.setPowerState("Started");
        deployment.setRoleInstanceList(Arrays.asList(roleInstance));
        deployment.setVirtualNetworkName("VLAN_NAME");

        HostedServiceModel.Role role = new HostedServiceModel.Role();
        role.setRoleName(VM_NAME);
        HostedServiceModel.ConfigurationSet configurationSet = new HostedServiceModel.ConfigurationSet();
        configurationSet.setSubnetNames(Arrays.asList("SUBNET_NAME"));
        role.setConfigurationSets(Arrays.asList(configurationSet));
        HostedServiceModel.OSVirtualHardDisk osVirtualHardDisk = new HostedServiceModel.OSVirtualHardDisk();
        osVirtualHardDisk.setSourceImageName("DISK_SOURCE_IMAGE_NAME");
        osVirtualHardDisk.setMediaLink("DISK_MEDIA_LINK");
        osVirtualHardDisk.setOs("DISK_OS");
        role.setOsVirtualHardDisk(osVirtualHardDisk);
        deployment.setRoleList(Arrays.asList(role));

        hostedServiceModel.setDeployments(Arrays.asList(deployment));
        return hostedServiceModel;
    }

    private void assertVirtualMachine(HostedServiceModel hostedServiceModel, MachineImage testMachineImage, VirtualMachine virtualMachine) throws URISyntaxException {
        assertEquals(Architecture.I64, virtualMachine.getArchitecture());
        assertEquals(false, virtualMachine.isClonable());
        assertEquals(false, virtualMachine.isImagable());
        assertEquals(true, virtualMachine.isPersistent());
        assertEquals(ACCOUNT_NO, virtualMachine.getProviderOwnerId());
        assertEquals(REGION, virtualMachine.getProviderRegionId());
        assertEquals(REGION, virtualMachine.getProviderDataCenterId());

        assertEquals(String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, VM_NAME), virtualMachine.getProviderVirtualMachineId());
        assertEquals(hostedServiceModel.getHostedServiceProperties().getAffinityGroup(), virtualMachine.getAffinityGroupId());
        HostedServiceModel.Deployment deployment = hostedServiceModel.getDeployments().get(0);
        HostedServiceModel.RoleInstance roleInstance = deployment.getRoleInstanceList().get(0);
        HostedServiceModel.Role role = deployment.getRoleList().get(0);
        assertEquals(roleInstance.getRoleName(), virtualMachine.getDescription());
        assertEquals(roleInstance.getRoleName(), virtualMachine.getName());
        assertTrue(virtualMachine.getPrivateAddresses() != null && virtualMachine.getPrivateAddresses().length == 1);
        assertEquals(roleInstance.getIpAddress(), virtualMachine.getPrivateAddresses()[0].getIpAddress());
        assertEquals(testMachineImage.getPlatform(), virtualMachine.getPlatform());
        assertEquals(roleInstance.getInstanceSize(), virtualMachine.getProductId());
        assertEquals(deployment.getRoleList().get(0).getOsVirtualHardDisk().getSourceImageName(), virtualMachine.getProviderMachineImageId());
        assertEquals(role.getConfigurationSets().get(0).getSubnetNames().get(0) + "_PROVIDER_VLAN_ID", virtualMachine.getProviderSubnetId());
        assertEquals("PROVIDER_VLAN_ID", virtualMachine.getProviderVlanId());
        assertEquals(new URI(deployment.getUrl()).getHost(), virtualMachine.getPublicDnsAddress());
        assertTrue(virtualMachine.getPublicAddresses() != null && virtualMachine.getPublicAddresses().length == 1);
        assertEquals(roleInstance.getInstanceEndpoints().get(0).getVip(), virtualMachine.getPublicAddresses()[0].getIpAddress());
    }
}
