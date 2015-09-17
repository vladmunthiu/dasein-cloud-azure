package org.dasein.cloud.azure.tests.compute.image;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.azure.AzureService;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.azure.compute.image.AzureOSImage;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import mockit.*;

public class AzureImageTest extends AzureTestsBase {
	
	private final String IMAGE_ID = "TESTIMAGEID";
	private final String IMAGE_NAME = "TESTIMAGENAME";
	
	private final String CAPTURE_IMAGE_URL = "%s/%s/services/hostedservices/%s/deployments/%s/roleInstances/%s/Operations";
	private final String REMOVE_VM_IMAGE_URL = "%s/%s/services/vmimages/%s?comp=media";
	private final String REMOVE_OS_IMAGE_URL = "%s/%s/services/images/%s?comp=media";
	
	@Mocked
	protected AzureComputeServices azureComputeServicesMock;
	@Mocked
	protected AzureVM azureVirtualMachineSupportMock;
	@Mocked
	protected VirtualMachine virtualMachineMock;
	
	@Rule
    public final TestName name = new TestName();
	
	@Before
	public void initExpectations() throws InternalException, CloudException {
        
		String methodName = name.getMethodName();
		
        if (methodName.startsWith("capture")) {
    		new NonStrictExpectations() { 
    			{ azureMock.getComputeServices(); result = azureComputeServicesMock; }
    			{ azureMock.hold(); }
    			{ azureComputeServicesMock.getVirtualMachineSupport(); result = azureVirtualMachineSupportMock; }
            };
            
            final AzureOSImage anyInstance = new AzureOSImage(azureMock);
	        new NonStrictExpectations() {
	        	{ azureVirtualMachineSupportMock.getVirtualMachine(anyString); result = virtualMachineMock; }
	        	{ virtualMachineMock.getProviderVirtualMachineId(); result = "TESTVMID"; }
	        	{ virtualMachineMock.getPlatform(); result = Platform.RHEL; }
	        	{ virtualMachineMock.getCurrentState(); result = VmState.STOPPED; }
	        	{ virtualMachineMock.getTag("serviceName"); result = SERVICE_NAME; }
	        	{ virtualMachineMock.getTag("deploymentName"); result = DEPLOYMENT_NAME; }
	        	{ virtualMachineMock.getTag("roleName"); result = ROLE_NAME; }
	        };
	        if (methodName.endsWith("RetrieveImageTimeout")) {
	        	new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = null; }
		        };
	        } else {
		        new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = MachineImage.getInstance(ACCOUNT_NO, REGION, IMAGE_ID, 
		        			ImageClass.MACHINE, MachineImageState.PENDING, IMAGE_NAME, IMAGE_NAME, Architecture.I64, Platform.RHEL); }
		        };
	        }
	        if (methodName.endsWith("TerminateServiceFailed")) {
	        	new Expectations() {
	    			{	azureVirtualMachineSupportMock.terminateService(anyString, anyString);
	    				result = new CloudException("Terminate service failed!"); }
	    		};
	        }
        } else if (methodName.startsWith("remove")) {
        	
        	final AzureOSImage anyInstance = new AzureOSImage(azureMock);
    		if (methodName.endsWith("WithCorrectRequest")) {
    			final AzureMachineImage azureMachineImage = new AzureMachineImage();
        		azureMachineImage.setAzureImageType("osimage");
        		azureMachineImage.setProviderMachineImageId(IMAGE_ID);
	        	if (methodName.contains("OS")) {
	        		azureMachineImage.setAzureImageType("osimage");
	        	} else if (methodName.contains("VM")) {
	        		azureMachineImage.setAzureImageType("vmimage");
	        	}
	        	new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = azureMachineImage; }
		        };
    		} else {
    			new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = null; }
		        };
    		}
        } else if (methodName.startsWith("isImageSharedWithPublic")) {
        	final AzureOSImage anyInstance = new AzureOSImage(azureMock);
        	if (methodName.endsWith("True")) {
        		new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = MachineImage.getInstance("--public--", REGION, IMAGE_ID, 
		        			ImageClass.MACHINE, MachineImageState.PENDING, IMAGE_NAME, IMAGE_NAME, Architecture.I64, Platform.RHEL); }
		        };
        	} else {
        		new NonStrictExpectations(AzureOSImage.class) {
		        	{ anyInstance.getImage(anyString); result = MachineImage.getInstance(ACCOUNT_NO, REGION, IMAGE_ID, 
		        			ImageClass.MACHINE, MachineImageState.PENDING, IMAGE_NAME, IMAGE_NAME, Architecture.I64, Platform.RHEL); }
		        };
        	}
        } else if (methodName.startsWith("isSubscribed")) {
        		new NonStrictExpectations() {
	    			{azureLocationMock.isSubscribed(AzureService.COMPUTE); result = true; }
	    			{azureLocationMock.isSubscribed((AzureService) any); result = false; }
	    		};
        } else if (methodName.startsWith("isSubscribed")) {
        	if (methodName.endsWith("True")) {
        		new NonStrictExpectations() {
        			{azureLocationMock.isSubscribed((AzureService) any); result = true; }
        		};
        	} else {
        		new NonStrictExpectations() {
        			{azureLocationMock.isSubscribed((AzureService) any); result = false; }
        		};
        	}
        }
	}
	
	@Before
	public void initMockUps() {
		
		final String methodName = name.getMethodName();
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK), 
				null, 
				new Header[]{});
		
		if (methodName.startsWith("capture")) {
			new MockUp<CloseableHttpClient>() {
	            @Mock(invocations = 1)
	            public CloseableHttpResponse execute(HttpUriRequest request) {
	        		assertPost(request, 
	        				String.format(CAPTURE_IMAGE_URL, ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME));
	            	return responseMock;
	            }
	        };
		} else if (methodName.startsWith("remove") && methodName.endsWith("WithCorrectRequest")) {
			if (methodName.contains("OS")) {
				new MockUp<CloseableHttpClient>() {
		            @Mock(invocations = 1)
		            public CloseableHttpResponse execute(HttpUriRequest request) {
		        		assertDelete(request, String.format(REMOVE_OS_IMAGE_URL, ENDPOINT, ACCOUNT_NO, IMAGE_ID));
		            	return responseMock;
		            }
		        };
			} else if (methodName.contains("VM")) {
				new MockUp<CloseableHttpClient>() {
		            @Mock(invocations = 1)
		            public CloseableHttpResponse execute(HttpUriRequest request) {
		        		assertDelete(request, String.format(REMOVE_VM_IMAGE_URL, ENDPOINT, ACCOUNT_NO, IMAGE_ID));
		            	return responseMock;
		            }
		        };
			}
		} 
	}
	
	/*
	 * capture image
	 */
	@Test
	public void captureWithOptionShouldPostWithCorrectRequest() throws CloudException, InternalException {
        
        final AzureOSImage support = new AzureOSImage(azureMock);
     
        ImageCreateOptions options = ImageCreateOptions.getInstance(virtualMachineMock, IMAGE_NAME, IMAGE_NAME);
        MachineImage image = support.captureImage(options);
        
        assertNotNull("Capture image returns null image", image);
        assertEquals("Capture image returns invalid image id", IMAGE_ID, image.getProviderMachineImageId());
	}
	
	@Test
	public void captureWithTaskShouldPostWithCorrectRequest() throws CloudException, InternalException, InterruptedException {
		
		final AzureOSImage support = new AzureOSImage(azureMock);
		final AtomicBoolean taskRun = new AtomicBoolean(false);
		
		AsynchronousTask<MachineImage> task = new AsynchronousTask<MachineImage>() {
			@Override
			public synchronized void completeWithResult(@Nullable MachineImage result) {
				super.completeWithResult(result);
				assertNotNull("Capture image returns null image", result);
				assertEquals("Capture image returns invalid image id", IMAGE_ID, result.getProviderMachineImageId());
				taskRun.compareAndSet(false, true);
			}
		};
		
		ImageCreateOptions options = ImageCreateOptions.getInstance(virtualMachineMock, IMAGE_NAME, IMAGE_NAME);
        support.captureImageAsync(options, task);
		while(!task.isComplete()) {
			Thread.sleep(1000);
		}
        assertTrue("Capture with task doesn't have a task run", taskRun.get());
	}
	
	@Test(expected = CloudException.class)
	public void captureShouldThrowExceptionIfRetrieveImageTimeout() throws CloudException, InternalException {
		
		AzureOSImage support = new AzureOSImage(azureMock);
		ImageCreateOptions options = ImageCreateOptions.getInstance(virtualMachineMock, IMAGE_NAME, IMAGE_NAME);
		support.captureImage(options);
	}
	
	@Test(expected = CloudException.class)
	public void captureShouldThrowExceptionIfTerminateServiceFailed() throws InternalException, CloudException {
		
		AzureOSImage support = new AzureOSImage(azureMock);
		ImageCreateOptions options = ImageCreateOptions.getInstance(virtualMachineMock, IMAGE_NAME, IMAGE_NAME);
		support.captureImage(options);
	}
	
	
	/*
	 * remove image
	 */
	@Test
	public void removeVMImageShouldDeleteWithCorrectRequest() throws CloudException, InternalException {
		new AzureOSImage(azureMock).remove(IMAGE_ID);
	}
	
	@Test
	public void removeOSImageShouldDeleteWithCorrectRequest() throws CloudException, InternalException {
		new AzureOSImage(azureMock).remove(IMAGE_ID);
	}
	
	@Test(expected = CloudException.class)
	public void removeShouldThrowExceptionIfRetrieveImageFailed() throws CloudException, InternalException {
		new AzureOSImage(azureMock).remove(IMAGE_ID);
	}

	/*
	 * others
	 */
	@Test
	public void getProviderTermByLocaleShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals("Provider Term is invalid", "OS image", 
				new AzureOSImage(azureMock).getProviderTermForImage(Locale.ENGLISH));
	}
	
	@Test
	public void getProviderTermByLocaleAndClassShouldReturnCorrectResult() {
		assertEquals("Provider Term is invalid", "OS image", 
				new AzureOSImage(azureMock).getProviderTermForImage(Locale.ENGLISH, ImageClass.MACHINE));
	}
	
	@Test
	public void getProviderTermForCustomImageShouldReturnCorrectResult() {
		assertEquals("Provider Term is invalid", "OS image", 
				new AzureOSImage(azureMock).getProviderTermForCustomImage(Locale.ENGLISH, ImageClass.MACHINE));
	}
	
	@Test
	public void hasPublicLibraryShouldReturnCorrectResult() {
		assertTrue("check for hasPublicLibrary returns false", new AzureOSImage(azureMock).hasPublicLibrary());
	}
	
	@Test
	public void identifyLocalBundlingRequirementShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals("identifyLocalBundlingRequirement return invalid result", 
				Requirement.NONE, new AzureOSImage(azureMock).identifyLocalBundlingRequirement());
	}
	
	@Test
	public void isImageSharedWithPublicShouldReturnTrue() throws CloudException, InternalException {
		assertTrue("image share with public match provider owner --public-- but returns false", 
				new AzureOSImage(azureMock).isImageSharedWithPublic(IMAGE_ID));
	}
	
	@Test
	public void isImageSharedWithPublicShouldReturnFalse() throws CloudException, InternalException {
		assertFalse("image share with public match a specific owner id but return true", 
				new AzureOSImage(azureMock).isImageSharedWithPublic(IMAGE_ID));
	}
	
	@Test
	public void isSubscribedShouldReturnTrue() throws CloudException, InternalException {
		assertTrue("subscribe compute returns false", new AzureOSImage(azureMock).isSubscribed());
	}
	
	@Test
	public void isSubscribedShouldReturnFalse() throws CloudException, InternalException {
		assertTrue("subscribe compute returns false", new AzureOSImage(azureMock).isSubscribed());
	}
	
	@Test
	public void listSharesShouldReturnEmptyList() throws CloudException, InternalException {
		Iterator<String> accounts = new AzureOSImage(azureMock).listShares(IMAGE_ID).iterator();
		assertFalse("list shares find more than one accounts shared", accounts.hasNext());
	}
	
	@Test
	public void supportsCustomImagesShouldReturnTrue() {
		assertTrue("supportsCustomImages returns false", new AzureOSImage(azureMock).supportsCustomImages());
	}
	
	@Test(expected=OperationNotSupportedException.class)
	public void registerImageBundleShouldThrowException() throws CloudException, InternalException {
		new AzureOSImage(azureMock).registerImageBundle(null);
	}
	
	@Test(expected=OperationNotSupportedException.class)
	public void removeImageShareShouldThrowException() throws CloudException, InternalException {
		new AzureOSImage(azureMock).removeImageShare(null, null);
	}
	
}
