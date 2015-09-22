package org.dasein.cloud.azure.tests.compute.disk;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;
import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.disk.AzureDisk;
import org.dasein.cloud.azure.compute.disk.model.AttachedToModel;
import org.dasein.cloud.azure.compute.disk.model.DataVirtualHardDiskModel;
import org.dasein.cloud.azure.compute.disk.model.DiskModel;
import org.dasein.cloud.azure.compute.disk.model.DisksModel;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.compute.vm.model.DeploymentModel;
import org.dasein.cloud.azure.compute.vm.model.DeploymentModel.RoleModel;
import org.dasein.cloud.azure.tests.AzureTestsBaseWithLocation;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFilterOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeType;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Storage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

public class AzureVolumeTest extends AzureTestsBaseWithLocation {

	private final String DISK_SERVICES = "%s/%s/services/disks";
    private final String HOSTED_SERVICES = "%s/%s/services/hostedservices";
    private final String DEPLOYMENT_RESOURCE = "%s/%s/services/hostedservices/%s/deployments/%s";
    private final String DATA_DISK_LUN = "%s/%s/services/hostedservices/%s/deployments/%s/roles/%s/DataDisks/%s";
    private final String DATA_DISK_RESOURCE = "%s/%s/services/hostedservices/%s/deployments/%s/roles/%s/DataDisks";
    
    private final String VOLUME_ID = "TEST_VOLUME";
    private final String GET_VOLUME_ID = VOLUME_ID + "_GET";
    private final String DEVICE_ID = "1";
    
    @Mocked
	protected AzureComputeServices azureComputeServicesMock;
	@Mocked
	protected AzureVM azureVirtualMachineSupportMock;
	@Mocked
	protected AffinityGroupSupport azureAffinityGroupSupportMock;
	@Mocked
	protected VirtualMachine virtualMachineMock;
	@Mocked
	protected AffinityGroup affinityGroupMock;
    
    @Rule
    public final TestName name = new TestName();
    
    @Before
    public void initExpectations() throws InternalException, CloudException {
    	
    	final String methodName = name.getMethodName();
    	if (methodName.startsWith("attach") || methodName.startsWith("detach") || methodName.startsWith("createVolume")) {
    		
    		new NonStrictExpectations() {
    			{ azureMock.getComputeServices(); result = azureComputeServicesMock; }
    			{ azureComputeServicesMock.getVirtualMachineSupport(); result = azureVirtualMachineSupportMock; }
            };
            
            if (methodName.startsWith("createVolume")) {
            	new NonStrictExpectations() {
            		{ azureMock.getStorageEndpoint(); result = "TESTSTORAGEENDPOINT"; }
            	};
            }

            if (!methodName.endsWith("NoServerFound")) {
		        new NonStrictExpectations() {
		        	{ azureVirtualMachineSupportMock.getVirtualMachine(anyString); result = virtualMachineMock; }
		        	{ virtualMachineMock.getTag("serviceName"); result = SERVICE_NAME; }
		        	{ virtualMachineMock.getTag("deploymentName"); result = DEPLOYMENT_NAME; }
		        	{ virtualMachineMock.getTag("roleName"); result = ROLE_NAME; }
		        };
            } else {
            	new NonStrictExpectations() {
		        	{ azureVirtualMachineSupportMock.getVirtualMachine(anyString); result = null; }
            	};
            }
    	} else if (methodName.startsWith("listVolume") || methodName.startsWith("getVolume")) {
    		
    		new NonStrictExpectations() {
    			{ azureMock.getComputeServices(); result = azureComputeServicesMock; }
    			{ azureComputeServicesMock.getAffinityGroupSupport(); result = azureAffinityGroupSupportMock; }
    			{ azureAffinityGroupSupportMock.get(anyString); result = affinityGroupMock; }
    			{ affinityGroupMock.getDataCenterId(); result = REGION; }
            };
    	} 
    }
    
    @Before
    public void initMockUps() {
    	
    	final String methodName = name.getMethodName();
    	
    	if (methodName.startsWith("remove") && methodName.endsWith("WithCorrectRequest")) {
    		new MockUp<System>() {
        		@Mock
        		long currentTimeMillis(Invocation inv) {
        			return CalendarWrapper.MINUTE * 8L * inv.getInvocationCount();
        		}
        	};
    	} else if (methodName.startsWith("listVolumes") || (methodName.startsWith("getVolume") && !methodName.startsWith("getVolumeProduct"))) {

    		if (!methodName.endsWith("NoVolumeFound")) {
	    		
    			final DisksModel disksModel = new DisksModel();
	    		disksModel.setDisks(Arrays.asList(
	    				createDiskModel(VOLUME_ID), 
	    				createDiskModel(GET_VOLUME_ID)));
	    		
	    		final DataVirtualHardDiskModel dataVirtualHardDiskModel = new DataVirtualHardDiskModel();
	    		dataVirtualHardDiskModel.setLun(DEVICE_ID);
	    		
	    		new MockUp<CloseableHttpClient>() {
	                @Mock(invocations = 3)
	                public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
	                	if (inv.getInvocationCount() == 1) {
	                		assertGet(request, String.format(DISK_SERVICES, ENDPOINT, ACCOUNT_NO));
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DisksModel>(disksModel),
	                				new Header[]{});
	                	} else if (inv.getInvocationCount() == 2) {
	                		assertGet(request, String.format(HOSTED_SERVICES + "/%s/deployments/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
	                		dataVirtualHardDiskModel.setDiskName(VOLUME_ID);
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DataVirtualHardDiskModel>(dataVirtualHardDiskModel),
	                				new Header[]{});
	                	} else if (inv.getInvocationCount() == 3) {
	                		assertGet(request, String.format(HOSTED_SERVICES + "/%s/deployments/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
	                		dataVirtualHardDiskModel.setDiskName(GET_VOLUME_ID);
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DataVirtualHardDiskModel>(dataVirtualHardDiskModel),
	                				new Header[]{});
	                	} else {
	                		throw new RuntimeException("Invalid invocation count!");
	                	}
	                }
	            };
    		} else {
    			new MockUp<CloseableHttpClient>() {
	                @Mock(invocations = 1)
	                public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
	                	if (inv.getInvocationCount() == 1) {
	                		assertGet(request, String.format(DISK_SERVICES, ENDPOINT, ACCOUNT_NO));
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DisksModel>(new DisksModel()),
	                				new Header[]{});
	                	} else {
	                		throw new RuntimeException("Invalid invocation count!");
	                	}
	                }
	            };
    		}
    	} else if (methodName.startsWith("listVolumeStatus")) {
    		
    		if (!methodName.endsWith("NoVolumeFound")) {
    		
	    		final DisksModel disksModel = new DisksModel();
	    		disksModel.setDisks(Arrays.asList(
	    				createDiskModel(VOLUME_ID), 
	    				createDiskModel(GET_VOLUME_ID)));
	    		
	    		new MockUp<CloseableHttpClient>() {
	                @Mock(invocations = 1)
	                public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
	                	if (inv.getInvocationCount() == 1) {
	                		assertGet(request, String.format(DISK_SERVICES, ENDPOINT, ACCOUNT_NO));
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DisksModel>(disksModel),
	                				new Header[]{});
	                	} else {
	                		throw new RuntimeException("Invalid invocation count!");
	                	}
	                }
	            };
    		} else {
    			new MockUp<CloseableHttpClient>() {
	                @Mock(invocations = 1)
	                public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
	                	if (inv.getInvocationCount() == 1) {
	                		assertGet(request, String.format(DISK_SERVICES, ENDPOINT, ACCOUNT_NO));
	                		return getHttpResponseMock(
	                				getStatusLineMock(HttpServletResponse.SC_OK),
	                				new DaseinObjectToXmlEntity<DisksModel>(new DisksModel()),
	                				new Header[]{});
	                	} else {
	                		throw new RuntimeException("Invalid invocation count!");
	                	}
	                }
	            };
    		}
    	}
    	
    }
    
	@Test
	public void attachShouldPostWithCorrectRequest() throws InternalException, CloudException {
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertPost(request, String.format(DATA_DISK_RESOURCE, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME));
            	return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
            }
        };
		
        new AzureVolumeSupport(azureMock, new Volume()).attach(VOLUME_ID, "TESTSERVER", "TESTDEVICE");
	}
	
	@Test(expected = InternalException.class) 
	public void attachShouldThrowExceptionIfVolumIdIsNull() throws InternalException, CloudException {
		new AzureDisk(azureMock).attach(null, "TESTSERVER", "TESTDEVICE");
	}
	
	@Test(expected = InternalException.class)
	public void attachShouldThrowExceptionIfNoVolumeFound() throws InternalException, CloudException {
		new AzureVolumeSupport(azureMock, null).attach(VOLUME_ID, "TESTSERVER", "TESTDEVICE");
	}
	
	@Test(expected = InternalException.class)
	public void attachShouldThrowExceptionIfServerIdIsNull() throws InternalException, CloudException {
		new AzureVolumeSupport(azureMock, new Volume()).attach(VOLUME_ID, null, "TESTDEVICE");
	}
	
	@Test(expected = InternalException.class)
	public void attachShouldThrowExceptionIfNoServerFound() throws InternalException, CloudException {
		new AzureVolumeSupport(azureMock, new Volume()).attach(VOLUME_ID, "TESTSERVER", "TESTDEVICE");
	}
	
	@Test
	public void detachShouldDeleteWithCorrectRequest() throws InternalException, CloudException {
		
		final DataVirtualHardDiskModel dataVirtualHardDiskModel = new DataVirtualHardDiskModel();
		dataVirtualHardDiskModel.setLun(DEVICE_ID);
		dataVirtualHardDiskModel.setDiskName(VOLUME_ID);
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(HOSTED_SERVICES + "/%s/deployments/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
            		return getHttpResponseMock(
            				getStatusLineMock(HttpServletResponse.SC_OK),
            				new DaseinObjectToXmlEntity<DataVirtualHardDiskModel>(dataVirtualHardDiskModel),
            				new Header[]{});
            	} else if (inv.getInvocationCount() == 2) {
            		assertDelete(request, String.format(HOSTED_SERVICES + "/%s/deployments/%s/roles/%s/DataDisks/%s", 
            				ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME, DEVICE_ID));
            		return getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
        
        Volume volume = new Volume();
		volume.setProviderVolumeId(VOLUME_ID);
		volume.setProviderVirtualMachineId(SERVICE_NAME + ":" + DEPLOYMENT_NAME + ":" + ROLE_NAME);
		new AzureVolumeSupport(azureMock, volume).detach(VOLUME_ID, false);
	}
	
	@Test(expected = InternalException.class)
	public void detachShouldThrowExceptionIfVolumIdIsNull() throws InternalException, CloudException {
		new AzureDisk(azureMock).detach(null, false);
	}
	
	@Test(expected = InternalException.class)
	public void detachShouldThrowExceptionIfNoVolumeFound() throws InternalException, CloudException {
		new AzureVolumeSupport(azureMock, null).detach(VOLUME_ID, false);
	}
	
	@Test(expected = InternalException.class)
	public void detachShouldThrowExceptionIfNoServerFound() throws InternalException, CloudException {
		Volume volume = new Volume();
		volume.setProviderVirtualMachineId("TESTVM");
		new AzureVolumeSupport(azureMock, volume).detach(VOLUME_ID, false);
	}
	
	@Test(expected = InternalException.class)
	public void detachShouldThrowExceptionIfNoDiskLunFound() throws InternalException, CloudException {
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(HOSTED_SERVICES + "/%s/deployments/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
            		return getHttpResponseMock(
            				getStatusLineMock(HttpServletResponse.SC_OK),
            				new DaseinObjectToXmlEntity<DataVirtualHardDiskModel>(new DataVirtualHardDiskModel()),
            				new Header[]{});
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
        
        Volume volume = new Volume();
		volume.setProviderVolumeId(VOLUME_ID);
		volume.setProviderVirtualMachineId(SERVICE_NAME + ":" + DEPLOYMENT_NAME + ":" + ROLE_NAME);
		new AzureVolumeSupport(azureMock, volume).detach(VOLUME_ID, false);
	}

	@Test
	public void createVolumeShouldPostWithCorrectRequest() throws InternalException, CloudException {
		
		DataVirtualHardDiskModel dataVirtualHardDiskModel = new DataVirtualHardDiskModel();
		dataVirtualHardDiskModel.setDiskName(VOLUME_ID);
		RoleModel roleModel = new RoleModel();
		roleModel.setRoleName(ROLE_NAME);
		roleModel.setDataVirtualDisks(Arrays.asList(dataVirtualHardDiskModel));
		DeploymentModel deploymentModel = new DeploymentModel();
		deploymentModel.setRoles(Arrays.asList(roleModel));
		
		final CloseableHttpResponse getDataDisksCountResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DeploymentModel>(deploymentModel),
				new Header[]{});
		
		final CloseableHttpResponse getDeplaymentModelResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DataVirtualHardDiskModel>(dataVirtualHardDiskModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 3)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(DEPLOYMENT_RESOURCE, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
            		return getDataDisksCountResponseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertPost(request, String.format(DATA_DISK_RESOURCE, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME));
	            	return getDeplaymentModelResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
            		assertGet(request, String.format(DATA_DISK_LUN, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME, 2));
            		return getDeplaymentModelResponseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
        
		VolumeCreateOptions options = VolumeCreateOptions.getInstance(Storage.valueOf("10gb"), VOLUME_ID, VOLUME_ID)
				.withVirtualMachineId("TESTVM");
		String diskName = new AzureDisk(azureMock).createVolume(options);
		assertEquals("match disk name for createVolume failed", VOLUME_ID, diskName);
	}
	
	@Test(expected = InternalException.class)
	public void createVolumeShouldThrowExceptionIfNoVmIdFoundInOption() throws InternalException, CloudException {
		VolumeCreateOptions options = VolumeCreateOptions.getInstance(Storage.valueOf("10gb"), VOLUME_ID, VOLUME_ID);
		new AzureDisk(azureMock).createVolume(options);
	}
	
	@Test(expected = InternalException.class)
	public void createVolumeShouldThrowExceptionIfNoServerFound() throws InternalException, CloudException {
		VolumeCreateOptions options = VolumeCreateOptions.getInstance(Storage.valueOf("10gb"), VOLUME_ID, VOLUME_ID);
		new AzureDisk(azureMock).createVolume(options);
	}
	
	@Test(expected = InternalException.class)
	public void createVolumeShouldThrowExceptionIfReachMaximumDisksCount() throws InternalException, CloudException {
		
		RoleModel roleModel = new RoleModel();
		roleModel.setRoleName(ROLE_NAME);
		roleModel.setDataVirtualDisks(Arrays.asList(new DataVirtualHardDiskModel(), new DataVirtualHardDiskModel()));
		DeploymentModel deploymentModel = new DeploymentModel();
		deploymentModel.setRoles(Arrays.asList(roleModel));
		
		final CloseableHttpResponse getDataDisksCountResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DeploymentModel>(deploymentModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(DEPLOYMENT_RESOURCE, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME));
            		return getDataDisksCountResponseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
		
		VolumeCreateOptions options = VolumeCreateOptions.getInstance(Storage.valueOf("10gb"), VOLUME_ID, VOLUME_ID)
				.withVirtualMachineId("TESTVM");
		String diskName = new AzureDisk(azureMock).createVolume(options);
		assertEquals("match disk name for createVolume failed", VOLUME_ID, diskName);
	}

	@Test
	public void removeShouldDeleteWithCorrectRequest() throws InternalException, CloudException {
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertDelete(request, String.format(DISK_SERVICES+"/%s?comp=media", ENDPOINT, ACCOUNT_NO, VOLUME_ID));
            		return getHttpResponseMock(
            				getStatusLineMock(HttpServletResponse.SC_OK),
            				null,
            				new Header[]{});
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
        new AzureDisk(azureMock).remove(VOLUME_ID);
	}

	@Test
	public void listVolumesShouldReturnCorrectResult() throws InternalException, CloudException {
		Iterator<Volume> volumes = new AzureDisk(azureMock).listVolumes().iterator();
		assertTrue("find volumes returns empty list", volumes.hasNext());
		Volume expectedVolume = createExpectedVolume(VOLUME_ID);
		assertReflectionEquals("match fields failed for volume with id " + VOLUME_ID, expectedVolume, volumes.next());
		expectedVolume = createExpectedVolume(GET_VOLUME_ID);
		assertReflectionEquals("match fields failed for volume with id " + GET_VOLUME_ID, expectedVolume, volumes.next());
		assertFalse("more than expected volumes found", volumes.hasNext());
	}
	
	@Test
	public void listVolumesShouldReturnEmptyListIfNoVolumeFound() throws InternalException, CloudException {
		Iterator<Volume> volumes = new AzureDisk(azureMock).listVolumes().iterator();
		assertFalse("find more than one volume from the empty result list", volumes.hasNext());
	}
	
	@Test
	public void listVolumesByFilterShouldReturnCorrectResult() throws InternalException, CloudException {
		VolumeFilterOptions options = VolumeFilterOptions.getInstance();						//TODO, filter has no effect
		Iterator<Volume> volumes = new AzureDisk(azureMock).listVolumes(options).iterator();
		assertTrue("find volumes returns empty list", volumes.hasNext());
		Volume expectedVolume = createExpectedVolume(VOLUME_ID);
		assertReflectionEquals("match fields failed for volume with id " + VOLUME_ID, expectedVolume, volumes.next());
		expectedVolume = createExpectedVolume(GET_VOLUME_ID);
		assertReflectionEquals("match fields failed for volume with id " + GET_VOLUME_ID, expectedVolume, volumes.next());
		assertFalse("more than expected volumes found", volumes.hasNext());
	}
	
	@Test
	public void listVolumesByFilterShouldReturnEmptyListIfNoVolumeFound() throws InternalException, CloudException {
		VolumeFilterOptions options = VolumeFilterOptions.getInstance();						//TODO, filter has no effect
		Iterator<Volume> volumes = new AzureDisk(azureMock).listVolumes(options).iterator();
		assertFalse("find more than one volume from the empty result list", volumes.hasNext());
	}
	
	@Test
	public void listVolumeStatusShouldReturnCorrectResult() throws InternalException, CloudException {
		Iterator<ResourceStatus> resourceStatuses = new AzureDisk(azureMock).listVolumeStatus().iterator();
		assertTrue("find resource status for volume return empty list", resourceStatuses.hasNext());
		assertReflectionEquals("match value of resource status failed for volume with id " + VOLUME_ID, 
				new ResourceStatus(VOLUME_ID, VolumeState.AVAILABLE), resourceStatuses.next());
		assertReflectionEquals("match value of resource status failed for volume with id " + GET_VOLUME_ID, 
				new ResourceStatus(GET_VOLUME_ID, VolumeState.AVAILABLE), resourceStatuses.next());
		assertFalse("more than expected volume status found", resourceStatuses.hasNext());
	}
	
	@Test
	public void listVolumeStatusShouldReturnEmptyListIfNoVolumeFound() throws InternalException, CloudException {
		Iterator<ResourceStatus> resourceStatuses = new AzureDisk(azureMock).listVolumeStatus().iterator();
		assertFalse("find more than one volume from the empty result list", resourceStatuses.hasNext());
	}
	
	@Test
	public void getVolumeShouldReturnCorrectResult() throws InternalException, CloudException {
		Volume volume = new AzureDisk(azureMock).getVolume(GET_VOLUME_ID);
		assertNotNull("failed to retrieved volume by id " + GET_VOLUME_ID, volume);
		Volume expectedVolume = createExpectedVolume(GET_VOLUME_ID);
		assertReflectionEquals("match fields of volume failed", expectedVolume, volume);
	}
	
	@Test
	public void getVolumeShouldReturnNullIfNoVolumeFound() throws InternalException, CloudException {
		Volume volume = new AzureDisk(azureMock).getVolume(GET_VOLUME_ID);
		assertNull("target volume is not null", volume);
	}
	
	@Test
	public void getMaximumVolumeCountShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals("maximum volume count is incorrect", 16, new AzureDisk(azureMock).getMaximumVolumeCount());
	}
	
	@Test
	public void getMaximumVolumeSizeShouldReturnCorrectResult() throws InternalException, CloudException {
		AzureDisk support = new AzureDisk(azureMock);
		assertEquals("number of maximum volume size is incorrect", 1024, support.getMaximumVolumeSize().getQuantity().intValue());
		assertEquals("unit of maximum volume size is incorrect", Storage.GIGABYTE, support.getMaximumVolumeSize().getUnitOfMeasure());
	}
	
	@Test
	public void getMinimumVolumeSizeShouldReturnCorrectResult() throws InternalException, CloudException {
		AzureDisk support = new AzureDisk(azureMock);
		assertEquals("number of minimum volume size is incorrect", 1, support.getMinimumVolumeSize().getQuantity().intValue());
		assertEquals("unit of minimum volume size is incorrect", Storage.GIGABYTE, support.getMaximumVolumeSize().getUnitOfMeasure());
	}
	
	@Test
	public void getProviderTermForVolumeShouldReturnCorrectResult() {
		assertEquals("match provider term for volume is failed", "disk", new AzureDisk(azureMock).getProviderTermForVolume(null));
	}
	
	@Test
	public void getVolumeProductRequirementShouldReturnCorrectResult() throws InternalException, CloudException {
		assertEquals("match volume product requirement failed", Requirement.NONE, new AzureDisk(azureMock).getVolumeProductRequirement());
	}
	
	@Test
	public void isVolumeSizeDeterminedByProductShouldReturnCorrectResult() throws InternalException, CloudException {
		assertFalse("volume size determined by prodcut is incorrect", new AzureDisk(azureMock).isVolumeSizeDeterminedByProduct());
	}
	
	@Test //TODO check
	public void listPossibleDeviceIdsShouldReturnCorrectResult() throws InternalException, CloudException {
		Iterator<String> possibleDeviceIds = new AzureDisk(azureMock).listPossibleDeviceIds(null).iterator();
		for (int index = 0; index < 16; index++) {
			assertEquals("match device id failed", String.valueOf(index), possibleDeviceIds.next());
		}
		assertFalse("possible device id list is not empty", possibleDeviceIds.hasNext());
	}
	
	@Test
	public void listSupportedFormatsShouldReturnCorrectResult() throws InternalException, CloudException {
		Iterator<VolumeFormat> vfs = new AzureDisk(azureMock).listSupportedFormats().iterator();
		assertEquals("match supported formats failed", VolumeFormat.BLOCK, vfs.next());
		assertFalse("find more than one supported format for azure disk", vfs.hasNext());
	}
	
	@Test
	public void listVolumeProductsShouldReturnCorrectResult() throws InternalException, CloudException {
		assertFalse("volume product list is not empty", new AzureDisk(azureMock).listVolumeProducts().iterator().hasNext());
	}
	
	private DiskModel createDiskModel(String volumeId) {
    	
    	AttachedToModel attachedToModel = new AttachedToModel();
		attachedToModel.setDeploymentName(DEPLOYMENT_NAME);
		attachedToModel.setHostedServiceName(SERVICE_NAME);
		attachedToModel.setRoleName(ROLE_NAME);
		
    	DiskModel diskModel = new DiskModel();
    	diskModel.setAffinityGroup("TEST_AFFINITY_GROUP");
    	diskModel.setAttachedTo(attachedToModel);
    	diskModel.setLocation(REGION);
    	diskModel.setLogicalDiskSizeInGB("1000");
    	diskModel.setMediaLink("TEST_MEDIA_LINK");
    	diskModel.setName(volumeId);
    	diskModel.setSourceImageName("TEST_SOURCE_IMAGE_NAME");
		return diskModel;
    }
	
	private Volume createExpectedVolume(String volumeId) {
		
		Volume volume = new Volume();
		volume.setProviderRegionId(REGION);
		volume.setCurrentState(VolumeState.AVAILABLE);
		volume.setType(VolumeType.HDD);
		volume.setProviderVirtualMachineId(SERVICE_NAME + ":" + DEPLOYMENT_NAME + ":" + ROLE_NAME);
		volume.setGuestOperatingSystem(Platform.UNKNOWN);
		volume.setDataCenterId(REGION);
		volume.setSize(Storage.valueOf("1000gb"));
		volume.setMediaLink("TEST_MEDIA_LINK");
		volume.setProviderVolumeId(volumeId);
		volume.setDescription(volumeId);
		volume.setName(volumeId);
		volume.setDeviceId(DEVICE_ID);
		volume.setProviderSnapshotId("TEST_SOURCE_IMAGE_NAME");
		return volume;
	}
	
}
