package org.dasein.cloud.azure.tests;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;
import org.dasein.cloud.Cloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.AzureSSLSocketFactory;
import org.dasein.cloud.azure.AzureX509;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.azure.compute.vm.model.HostedServiceModel;
import org.dasein.cloud.compute.*;
import org.junit.Before;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by vmunthiu on 9/7/2015.
 */
public class AzureTestsBase {
    @Mocked protected ProviderContext providerContextMock;
    @Mocked protected Azure azureMock;
    @Mocked protected AzureSSLSocketFactory azureSSLSocketFactoryMock;
    @Mocked protected AzureX509 azureX509Mock;
    @Mocked protected Logger logger;
    @Mocked protected Cloud cloudMock;

    //Global
    protected final String ENDPOINT = "TESTENDPOINT";
    protected final String ACCOUNT_NO = "TESTACCOUNTNO";
    protected final String REGION = "TESTREGION";
    protected final String STORAGE_ENDPOINT = "TESTSTORAGEENDPOINT";

    //VM
    protected final String SERVICE_NAME = "TESTSERVICENAME";
    protected final String DEPLOYMENT_NAME = "TESTDEPLOYMENTNAME";
    protected final String ROLE_NAME = "TESTROLENAME";
    protected final String VM_NAME = "TESTVMNAME";
    protected final String VM_ID = String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

    @Before
    public void setUp() throws CloudException, InternalException {
        new NonStrictExpectations() {
            { azureMock.getContext(); result = providerContextMock; }
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
            { providerContextMock.getCloud(); result = cloudMock; }
            { cloudMock.getEndpoint(); result = ENDPOINT; }
        };
    }

    protected StatusLine getStatusLineMock(final int statusCode){
        return new MockUp<StatusLine>(){
            @Mock
            public int getStatusCode() {
                return statusCode;
            }
        }.getMockInstance();
    }

    protected CloseableHttpResponse getHttpResponseMock(final StatusLine statusLine, final HttpEntity httpEntity, final Header[] headers){
        return new MockUp<CloseableHttpResponse>(){
            @Mock
            public StatusLine getStatusLine() {
                return statusLine;
            }

            @Mock
            public HttpEntity getEntity() {
                return httpEntity;
            }

            @Mock
            public Header[] getAllHeaders() {
                return headers;
            }
            
            @Mock
            public Header getFirstHeader(String name) {
				for (Header header : headers) {
					if (header.getName().equals(name)) {
						return header;
					}
				}
				return null;
            }
        }.getMockInstance();
    }

    protected HostedServiceModel getHostedServiceModel(String affinityGroupId) {
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
        roleInstance.setRoleName(DEPLOYMENT_NAME);
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
        role.setRoleName(DEPLOYMENT_NAME);
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

    protected void assertVirtualMachine(HostedServiceModel hostedServiceModel, MachineImage testMachineImage, VirtualMachine virtualMachine, String serviceName, String deploymentName, String vmName) throws URISyntaxException {
        assertEquals(Architecture.I64, virtualMachine.getArchitecture());
        assertEquals(false, virtualMachine.isClonable());
        assertEquals(false, virtualMachine.isImagable());
        assertEquals(true, virtualMachine.isPersistent());
        assertEquals(ACCOUNT_NO, virtualMachine.getProviderOwnerId());
        assertEquals(REGION, virtualMachine.getProviderRegionId());
        assertEquals(REGION, virtualMachine.getProviderDataCenterId());

        assertEquals(String.format("%s:%s:%s", serviceName, deploymentName, vmName).toLowerCase(), virtualMachine.getProviderVirtualMachineId().toLowerCase());
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

    protected AzureMachineImage getAzureMachineImage(Platform platfrom, String imageType) {
        final AzureMachineImage testMachineImage = new AzureMachineImage();
        testMachineImage.setProviderOwnerId(ACCOUNT_NO);
        testMachineImage.setProviderRegionId(REGION);
        testMachineImage.setProviderMachineImageId("DISK_SOURCE_IMAGE_NAME");
        testMachineImage.setImageClass(ImageClass.MACHINE);
        testMachineImage.setCurrentState(MachineImageState.ACTIVE);
        testMachineImage.setPlatform(platfrom);
        //this will make different info to be posted
        testMachineImage.setAzureImageType(imageType);
        return testMachineImage;
    }
}