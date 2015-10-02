package org.dasein.cloud.azure.tests.compute.vm;

import mockit.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.compute.AzureAffinityGroupSupport;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.compute.vm.model.ConfigurationSetModel;
import org.dasein.cloud.azure.compute.vm.model.CreateHostedServiceModel;
import org.dasein.cloud.azure.compute.vm.model.DeploymentModel;
import org.dasein.cloud.azure.compute.vm.model.HostedServiceModel;
import org.dasein.cloud.azure.network.AzureNetworkServices;
import org.dasein.cloud.azure.network.AzureVlanSupport;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.util.CalendarWrapper;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertDelete;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.assertPost;
import static org.junit.Assert.assertEquals;

/**
 * Created by vmunthiu on 9/30/2015.
 */
public class AzureVMLaunchTests extends AzureTestsBase {
    @Mocked AzureComputeServices computeServiceMock;
    @Mocked AzureAffinityGroupSupport affinitySupportMock;
    @Mocked AzureLocation dataCenterServiceMock;
    @Mocked AzureOSImage imageSupportMock;
    @Mocked AzureNetworkServices networkServicesMock;


    private static String HOSTED_SERVICE_URL = "%s/%s/services/hostedservices/%s";
    private static String HOSTED_SERVICES_URL = "%s/%s/services/hostedservices";
    private static String DEPLOYMENTS_SERVICE_URL = "%s/%s/services/hostedservices/%s/deployments";

    final String REGION_NAME = "TEST_REGION_NAME";
    private String affinityGroupId = "TEST_AFFINITY_ID";

    final String testusername = "TESTUSERNAME";
    final String testpassword = "TESTPASSWORD";

    @Before
    public void setUp() throws CloudException, InternalException {
        super.setUp();

        final DataCenter testDataCenter = new DataCenter(REGION, REGION_NAME, REGION, true, true);
        final AffinityGroup testAffinityGroup = AffinityGroup.getInstance(affinityGroupId,"AG_NAME", "AG_DESCRIPTION", REGION, null);
        new NonStrictExpectations() {
            { azureMock.getComputeServices(); result = computeServiceMock; }
            { computeServiceMock.getAffinityGroupSupport(); result = affinitySupportMock; }
            { computeServiceMock.getImageSupport(); result = imageSupportMock; }
            { affinitySupportMock.get(anyString); result = testAffinityGroup;}
            { azureMock.getDataCenterServices(); result = dataCenterServiceMock; }
            { dataCenterServiceMock.getDataCenter(anyString); result = testDataCenter;}
            { azureMock.getStorageEndpoint(); result = STORAGE_ENDPOINT; }
        };
    }



    @Test(expected = CloudException.class)
    public void launchThrowExceptionWhenImageNotFound() throws CloudException, InternalException {
        new NonStrictExpectations() {
            { imageSupportMock.getMachineImage("DISK_SOURCE_IMAGE_NAME"); result = null;}
        };

        VMLaunchOptions vmLaunchOptions = VMLaunchOptions.getInstance("Small", "DISK_SOURCE_IMAGE_NAME", DEPLOYMENT_NAME , DEPLOYMENT_NAME);
        final AzureVM azureVM = new AzureVM(azureMock);
        azureVM.launch(vmLaunchOptions);
    }

    @Test(expected = CloudException.class)
    public void launchCreateDeploymentFailsThrowExceptionAndDeletesHostedService() throws CloudException, InternalException {
        new NonStrictExpectations() {
            { imageSupportMock.getMachineImage("DISK_SOURCE_IMAGE_NAME"); result = getAzureMachineImage(Platform.WINDOWS, "vmimage");}
        };
        final CloseableHttpResponse postHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final CloseableHttpResponse getHttpResponseNullEntityMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format(HOSTED_SERVICE_URL, ENDPOINT, ACCOUNT_NO,DEPLOYMENT_NAME.toLowerCase());
        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 4)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException, CloudException {
                if (request.getMethod().equalsIgnoreCase("POST")) {
                    if (request.getURI().toString().endsWith("/services/hostedservices")) {
                        return postHttpResponseMock;
                    } else if (request.getURI().toString().endsWith("/deployments")) {
                        throw new CloudException("Deployment creation failed");
                    }
                } else if (request.getMethod().equalsIgnoreCase("GET")) {
                    return getHttpResponseNullEntityMock;
                }

                if (inv.getInvocationCount() == 4) {
                    assertDelete(request, expectedUrl);
                    return postHttpResponseMock;
                }
                return null;
            }
        };
        VMLaunchOptions vmLaunchOptions = VMLaunchOptions.getInstance("Small", "DISK_SOURCE_IMAGE_NAME", DEPLOYMENT_NAME , DEPLOYMENT_NAME);
        final AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.launch(vmLaunchOptions);
    }

    @Test
    public void testLaunchUnixFromVmImgNoVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
           { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.UNIX, null,null, null, testusername, testpassword);
    }


    @Test
    public void testLaunchWinFromVmImgNoVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.WINDOWS, null, null, null, testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromVmImgWithUserData(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.WINDOWS, null, null, "USER_DATA", testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromVmImgWithUserData(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.UNIX, null, null, "USER_DATA", testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromOsImgWithUserData(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.WINDOWS, null, null, "USER_DATA", testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromOsImgWithUserData(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.UNIX, null, null, "USER_DATA", testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromVmImgWithVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet(null); result = null; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.WINDOWS, null, "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromVmImgWithSubnet(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock, @Mocked final Subnet subnetMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet("SUBNET_NAME"); result = subnetMock; }
            { subnetMock.getName(); result = "SUBNET_NAME";}
            { subnetMock.getTags(); result = new HashMap<String,String>() {{ put("vlanName", "VLAN_NAME");}};}
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.WINDOWS, "SUBNET_NAME", "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromVmImgWithVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet(null); result = null; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.UNIX, null, "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromVmImgWithSubnet(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock, @Mocked final Subnet subnetMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet("SUBNET_NAME"); result = subnetMock; }
            { subnetMock.getName(); result = "SUBNET_NAME";}
            { subnetMock.getTags(); result = new HashMap<String,String>() {{ put("vlanName", "VLAN_NAME");}};}
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromVmImage(Platform.UNIX, "SUBNET_NAME", "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromOsImgWithVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet(null); result = null; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.WINDOWS, null, "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromOsImgWithSubnet(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock, @Mocked final Subnet subnetMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet("SUBNET_NAME"); result = subnetMock; }
            { subnetMock.getName(); result = "SUBNET_NAME";}
            { subnetMock.getTags(); result = new HashMap<String,String>() {{ put("vlanName", "VLAN_NAME");}};}
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.WINDOWS, "SUBNET_NAME", "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromOsImgWithVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet(null); result = null; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.UNIX, null, "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchUnixFromOsImgWithSubnet(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock, @Mocked final Subnet subnetMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { azureMock.getNetworkServices(); result = networkServicesMock; }
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getName(); result = "VLAN_NAME";}
            { vlanSupportMock.getSubnet("SUBNET_NAME"); result = subnetMock; }
            { subnetMock.getName(); result = "SUBNET_NAME";}
            { subnetMock.getTags(); result = new HashMap<String,String>() {{ put("vlanName", "VLAN_NAME");}};}
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.UNIX, "SUBNET_NAME", "VLAN_NAME", null, testusername, testpassword);
    }

    @Test
    public void testLaunchWinFromOsImgNoVlan(@Mocked final AzureVlanSupport vlanSupportMock, @Mocked final VLAN vlanMock) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException {
        new NonStrictExpectations() {
            { networkServicesMock.getVlanSupport(); result = vlanSupportMock; }
            { vlanSupportMock.getVlan("VLAN_NAME"); result = vlanMock; }
            { vlanMock.getProviderVlanId(); result = "PROVIDER_VLAN_ID";}
        };
        launchFromOsImage(Platform.WINDOWS, null, null, null, testusername, testpassword);
    }

    private void launchFromOsImage(Platform platform,String subnetName,String vlanName, String userData, String testusername, String testpassword ) throws UnsupportedEncodingException, InternalException, CloudException, URISyntaxException {
        //for OsImages we need to fake the current time as it's used for the vhd's file name
        new MockUp<System>() {
            @Mock
            long currentTimeMillis(Invocation inv) {
                Date fake = new Date(111,10,11);
                return fake.getTime();
            }
        };

        final CreateHostedServiceModel expectedHostedSrvModel = getTestCreateHostedServiceModel();
        final DeploymentModel expectedDeploymentModel = getTestDeploymentModel(platform, subnetName, vlanName, "osimage",userData, testusername, testpassword);

        VMLaunchOptions vmLaunchOptions = VMLaunchOptions.getInstance("Small", "DISK_SOURCE_IMAGE_NAME", DEPLOYMENT_NAME.toLowerCase(), VM_NAME.toLowerCase(), DEPLOYMENT_NAME.toLowerCase());
        vmLaunchOptions.withBootstrapUser(testusername, testpassword);
        if(vlanName != null) {
            vmLaunchOptions.inVlan(null, REGION, vlanName);
        }
        if( subnetName != null) {
            vmLaunchOptions.inSubnet(null, REGION, vlanName, subnetName);
        }
        executeLaunchAndAssert(expectedHostedSrvModel, expectedDeploymentModel, vmLaunchOptions, getAzureMachineImage(platform, "osimage"));
    }

    private void launchFromVmImage(Platform platform,String subnetName,String vlanName, String userData, String testusername, String testpassword ) throws UnsupportedEncodingException, InternalException, CloudException, URISyntaxException {
        final CreateHostedServiceModel expectedHostedSrvModel = getTestCreateHostedServiceModel();
        final DeploymentModel expectedDeploymentModel = getTestDeploymentModel(platform, subnetName, vlanName, "vmimage",userData, testusername, testpassword);

        VMLaunchOptions vmLaunchOptions = VMLaunchOptions.getInstance("Small", "DISK_SOURCE_IMAGE_NAME", DEPLOYMENT_NAME.toLowerCase(), VM_NAME.toLowerCase(), DEPLOYMENT_NAME.toLowerCase());
        vmLaunchOptions.withBootstrapUser(testusername, testpassword);
        if(vlanName != null) {
            vmLaunchOptions.inVlan(null, REGION, vlanName);
        }
        if( subnetName != null) {
            vmLaunchOptions.inSubnet(null, REGION, vlanName, subnetName);
        }
        executeLaunchAndAssert(expectedHostedSrvModel, expectedDeploymentModel, vmLaunchOptions, getAzureMachineImage(platform, "vmimage"));
    }

    private void executeLaunchAndAssert(final CreateHostedServiceModel expectedCreateHostedServiceModel, final DeploymentModel expectedDeploymentModel, VMLaunchOptions launchOptions, final AzureMachineImage testImage) throws CloudException, InternalException, URISyntaxException {
        new NonStrictExpectations() {
            { imageSupportMock.getMachineImage("DISK_SOURCE_IMAGE_NAME"); result = testImage;}
        };
        HostedServiceModel hostedServiceModel = getHostedServiceModel(affinityGroupId);

        final DaseinObjectToXmlEntity<HostedServiceModel> responseEntity = new DaseinObjectToXmlEntity<HostedServiceModel>(hostedServiceModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), responseEntity , new Header[]{});
        final CloseableHttpResponse getHttpResponseNullEntityMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final CloseableHttpResponse postHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});

        final String expectedCreateHostedServiceUrl = String.format(HOSTED_SERVICES_URL, ENDPOINT, ACCOUNT_NO);
        final String expectedCreateDeploymentUrl = String.format(DEPLOYMENTS_SERVICE_URL, ENDPOINT, ACCOUNT_NO, DEPLOYMENT_NAME.toLowerCase());

        new MockUp<CloseableHttpClient>() {
            @Mock
            public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                if(request.getMethod().equalsIgnoreCase("GET")) {
                    if(request.getURI().toString().endsWith("embed-detail=true"))
                        return getHttpResponseMock;
                    else
                        return getHttpResponseNullEntityMock;
                }
                else {
                    if (request.getURI().toString().endsWith("/services/hostedservices")) {
                        assertPost(request, expectedCreateHostedServiceUrl, new Header[]{new BasicHeader("x-ms-version", "2012-03-01")}, expectedCreateHostedServiceModel);
                    } else if(request.getURI().toString().endsWith("/deployments")) {
                        assertPost(request, expectedCreateDeploymentUrl, new Header[]{ new BasicHeader("x-ms-version", "2014-05-01")}, expectedDeploymentModel);
                    }
                    return postHttpResponseMock;
                }
            }
        };


        final AzureVM azureVM = new AzureVM(azureMock);
        VirtualMachine virtualMachine = azureVM.launch(launchOptions);
        assertVirtualMachine(hostedServiceModel, testImage, virtualMachine, DEPLOYMENT_NAME, DEPLOYMENT_NAME, DEPLOYMENT_NAME);
        assertEquals(launchOptions.getBootstrapUser(), virtualMachine.getRootUser());
        assertEquals(launchOptions.getBootstrapPassword(), virtualMachine.getRootPassword());
    }

    private DeploymentModel getTestDeploymentModel(Platform platform,String subnetName,String vlanName,String azureImageType, String userData, String username, String password ) throws UnsupportedEncodingException {
        final DeploymentModel expectedDeploymentModel = new DeploymentModel();
        expectedDeploymentModel.setName(DEPLOYMENT_NAME.toLowerCase());
        expectedDeploymentModel.setDeploymentSlot("Production");
        expectedDeploymentModel.setLabel(new String(Base64.encodeBase64(VM_NAME.toLowerCase().getBytes("utf-8"))));
        DeploymentModel.RoleModel expectedRoleModel = new DeploymentModel.RoleModel();
        expectedRoleModel.setRoleName(DEPLOYMENT_NAME.toLowerCase());
        expectedRoleModel.setRoleType("PersistentVMRole");

        ArrayList<ConfigurationSetModel> configurations = new ArrayList<ConfigurationSetModel>();
        if (platform.isWindows())
        {
            ConfigurationSetModel windowsConfigurationSetModel = new ConfigurationSetModel();
            windowsConfigurationSetModel.setConfigurationSetType("WindowsProvisioningConfiguration");
            windowsConfigurationSetModel.setType("WindowsProvisioningConfigurationSet");
            windowsConfigurationSetModel.setComputerName(DEPLOYMENT_NAME.toLowerCase());
            windowsConfigurationSetModel.setAdminPassword(password);
            windowsConfigurationSetModel.setEnableAutomaticUpdates("true");
            windowsConfigurationSetModel.setTimeZone("UTC");
            windowsConfigurationSetModel.setAdminUsername(username);
            if(userData != null && userData.equals("")) {
                windowsConfigurationSetModel.setCustomData(new String(Base64.encodeBase64(userData.getBytes())));
            }
            configurations.add(windowsConfigurationSetModel);
        }
        else
        {
            ConfigurationSetModel unixConfigurationSetModel = new ConfigurationSetModel();
            unixConfigurationSetModel.setConfigurationSetType("LinuxProvisioningConfiguration");
            unixConfigurationSetModel.setType("LinuxProvisioningConfigurationSet");
            unixConfigurationSetModel.setHostName(DEPLOYMENT_NAME.toLowerCase());
            unixConfigurationSetModel.setUserName(username);
            unixConfigurationSetModel.setUserPassword(password);
            unixConfigurationSetModel.setDisableSshPasswordAuthentication("false");
            if(userData != null && userData.equals("")) {
                unixConfigurationSetModel.setCustomData(new String(Base64.encodeBase64(userData.getBytes())));
            }
            configurations.add(unixConfigurationSetModel);

        }
        ConfigurationSetModel networkConfigurationSetModel = new ConfigurationSetModel();
        networkConfigurationSetModel.setConfigurationSetType("NetworkConfiguration");
        ArrayList<ConfigurationSetModel.InputEndpointModel> inputEndpointModels = new ArrayList<ConfigurationSetModel.InputEndpointModel>();
        if(platform.isWindows())
        {
            ConfigurationSetModel.InputEndpointModel inputEndpointModel = new ConfigurationSetModel.InputEndpointModel();
            inputEndpointModel.setLocalPort("3389");
            inputEndpointModel.setName("RemoteDesktop");
            inputEndpointModel.setPort("58622");
            inputEndpointModel.setProtocol("TCP");
            inputEndpointModels.add(inputEndpointModel);
        }
        else
        {
            ConfigurationSetModel.InputEndpointModel inputEndpointModel = new ConfigurationSetModel.InputEndpointModel();
            inputEndpointModel.setLocalPort("22");
            inputEndpointModel.setName("SSH");
            inputEndpointModel.setPort("22");
            inputEndpointModel.setProtocol("TCP");
            inputEndpointModels.add(inputEndpointModel);
        }
        networkConfigurationSetModel.setInputEndpoints(inputEndpointModels);
        if(subnetName != null)
        {
            ArrayList<String> subnets = new ArrayList<String>();
            subnets.add(subnetName);
            networkConfigurationSetModel.setSubnetNames(subnets);
        }
        configurations.add(networkConfigurationSetModel);

        expectedRoleModel.setConfigurationsSets(configurations);
        if(azureImageType.equalsIgnoreCase("osimage"))
        {
            DeploymentModel.OSVirtualHardDiskModel osVirtualHardDiskModel = new DeploymentModel.OSVirtualHardDiskModel();
            osVirtualHardDiskModel.setHostCaching("ReadWrite");
            osVirtualHardDiskModel.setDiskLabel("OS");
            String vhdFileName = String.format("%s-%s-%s-%s.vhd", DEPLOYMENT_NAME.toLowerCase(), DEPLOYMENT_NAME.toLowerCase(), DEPLOYMENT_NAME.toLowerCase(), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            osVirtualHardDiskModel.setMediaLink(STORAGE_ENDPOINT + "vhds/" + vhdFileName);
            osVirtualHardDiskModel.setSourceImageName("DISK_SOURCE_IMAGE_NAME");
            expectedRoleModel.setOsVirtualDisk(osVirtualHardDiskModel);
        }
        else if(azureImageType.equalsIgnoreCase("vmimage"))
        {
            expectedRoleModel.setVmImageName("DISK_SOURCE_IMAGE_NAME");
        }
        expectedRoleModel.setRoleSize("Small");

        ArrayList<DeploymentModel.RoleModel> roles = new ArrayList<DeploymentModel.RoleModel>();
        roles.add(expectedRoleModel);
        expectedDeploymentModel.setRoles(roles);

        if(vlanName != null)
        {
            expectedDeploymentModel.setVirtualNetworkName(vlanName);
        }
        return expectedDeploymentModel;
    }

    private CreateHostedServiceModel getTestCreateHostedServiceModel() throws UnsupportedEncodingException {
        final CreateHostedServiceModel expectedHostedSrvModel = new CreateHostedServiceModel();
        expectedHostedSrvModel.setServiceName(DEPLOYMENT_NAME.toLowerCase());
        expectedHostedSrvModel.setDescription(DEPLOYMENT_NAME.toLowerCase());
        expectedHostedSrvModel.setLabel(new String(Base64.encodeBase64(VM_NAME.toLowerCase().getBytes("utf-8"))));
        expectedHostedSrvModel.setLocation(REGION);
        return expectedHostedSrvModel;
    }
}
