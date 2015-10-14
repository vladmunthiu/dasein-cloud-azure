package org.dasein.cloud.azure;

import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;

import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;
import mockit.Mock;
import mockit.MockUp;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.model.Location;
import org.dasein.cloud.azure.model.Locations;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterCapabilities;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class AzureDataCenterServicesTest extends AzureTestsBase {

	private final String LOCATIONS = "%s/%s/locations";
	
	@Rule
    public final TestName name = new TestName();
	
	@Before
	public void initialize() {
		
		String methodName = name.getMethodName();
		
		if (methodName.startsWith("listRegions") || 
				methodName.startsWith("getRegion") || 
				methodName.startsWith("listDataCenters") || 
				methodName.startsWith("getDataCenter") || 
				methodName.startsWith("isSubscribed")) {
			
			Locations locations = new Locations();
			locations.setLocations(Arrays.asList(createLocation(REGION)));
			
			final CloseableHttpResponse responseMock = getHttpResponseMock(
					getStatusLineMock(HttpServletResponse.SC_OK),
					new DaseinObjectToXmlEntity<Locations>(locations),
					new Header[]{});
			
			new MockUp<CloseableHttpClient>() {
	            @Mock(invocations = 1)
	            public CloseableHttpResponse execute(HttpUriRequest request) {
	        		assertGet(request, String.format(LOCATIONS, ENDPOINT, ACCOUNT_NO));
	            	return responseMock;
	            }
			};
		} 
	}
	
	@After
	public void finalize() {
		
		Cache<Region> cache = Cache.getInstance(azureMock, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT);
		if (cache != null) {
			cache.clear();
		}
		
		String methodName = name.getMethodName();
		
		if (methodName.startsWith("isSubscribed")) {
			Cache<Boolean> computeServiceCache = Cache.getInstance(azureMock, 
					String.format("AzureLocation.isSubscribed.%s", AzureService.COMPUTE.toString()), 
					Boolean.class, CacheLevel.REGION_ACCOUNT);
			if (computeServiceCache != null) {
				computeServiceCache.clear();
			}
			Cache<Boolean> databaseServiceCache = Cache.getInstance(azureMock, 
					String.format("AzureLocation.isSubscribed.%s", AzureService.DATABASE.toString()), 
					Boolean.class, CacheLevel.REGION_ACCOUNT);
			if (databaseServiceCache != null) {
				databaseServiceCache.clear();
			}
		}
	}
	
	@Test
	public void getCapabilitiesShouldReturnCorrectResult() throws InternalException, CloudException {
		DataCenterCapabilities  capabilities = new AzureLocation(azureMock).getCapabilities();
		assertNotNull(capabilities);
		assertEquals(
				capabilities.getClass(), 
				AzureLocationCapabilities.class);
	}
	
	@Test
	public void getDataCenterShouldReturnCorrectResult() throws InternalException, CloudException {
		assertReflectionEquals(
				createDataCenter(REGION),
				new AzureLocation(azureMock).getDataCenter(REGION));
	}
	
	@Test
	public void getDataCenterShouldReturnCorrectResultIfNoDataCenterFound() throws AssertionFailedError, InternalException, CloudException {
		assertNull(new AzureLocation(azureMock).getDataCenter(REGION + "INVALID"));
	}
	
	@Test
	public void getRegionShouldReturnCorrectResult() throws InternalException, CloudException {
		assertReflectionEquals(
				createRegion(REGION), 
				new AzureLocation(azureMock).getRegion(REGION));
	}
	
	@Test
	public void listDataCentersShouldReturnCorrectResult() throws InternalException, CloudException {
		assertReflectionEquals(
				Arrays.asList(createDataCenter(REGION)),
				new AzureLocation(azureMock).listDataCenters(REGION));
	}
	
	@Test
	public void listDataCentersShouldReturnCorrectResultIfNoRegionFound() throws InternalException, CloudException {
		assertReflectionEquals(
				Collections.emptyList(),
				new AzureLocation(azureMock).listDataCenters(REGION + "INVALID"));
	}
	
	@Test
	public void listRegionsFromServerShouldReturnCorrectResult() throws InternalException, CloudException {
		assertReflectionEquals(
				Arrays.asList(createRegion(REGION)), 
				new AzureLocation(azureMock).listRegions());
	}
	
	@Test
	public void listRegionsFromCacheShouldReturnCorrectResult() throws InternalException, CloudException {
		new AzureLocation(azureMock).listRegions();
		assertReflectionEquals(
				Arrays.asList(createRegion(REGION)), 
				new AzureLocation(azureMock).listRegions());
	}
	
	@Test
	public void getProviderTermForDataCenterShouldReturnCorrectResult() {
		assertEquals(
				"data center", 
				new AzureLocation(azureMock).getProviderTermForDataCenter(null));
	}
	
	@Test
	public void getProviderTermForRegionShouldReturnCorrectResult() {
		assertEquals(
				"region", 
				new AzureLocation(azureMock).getProviderTermForRegion(null));
	}
	
	@Test
	public void listResourcePoolsShouldReturnCorrectResult() throws AssertionFailedError, InternalException, CloudException {
		assertReflectionEquals(
				Collections.emptyList(), 
				new AzureLocation(azureMock).listResourcePools(REGION));
	}
	
	@Test
	public void getResourcePoolShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull(new AzureLocation(azureMock).getResourcePool(null));
	}
	
	@Test
	public void listStoragePoolsShouldReturnCorrectResult() throws AssertionFailedError, InternalException, CloudException {
		assertReflectionEquals(
				Collections.emptyList(), 
				new AzureLocation(azureMock).listStoragePools());
	}
	
	@Test
	public void getStoragePoolShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull(new AzureLocation(azureMock).getStoragePool(null));
	}
	
	@Test
	public void listVMFoldersShouldReturnCorrectResult() throws AssertionFailedError, InternalException, CloudException {
		assertReflectionEquals(
				Collections.emptyList(), 
				new AzureLocation(azureMock).listVMFolders());
	}
	
	@Test
	public void isSubscribedFromServerShouldReturnCorrectResultIfServiceSubscribed() throws InternalException, CloudException {
		assertTrue(new AzureLocation(azureMock).isSubscribed(AzureService.COMPUTE));
	}
	
	@Test
	public void isSubscribedFromServerShouldReturnCorrectResultIfServiceNotSubscribed() throws InternalException, CloudException {
		assertFalse(new AzureLocation(azureMock).isSubscribed(AzureService.DATABASE));
	}
	
	@Test
	public void isSubscribedFromCacheShouldReturnCorrectResult() throws InternalException, CloudException {
		new AzureLocation(azureMock).isSubscribed(AzureService.COMPUTE);
		assertTrue(new AzureLocation(azureMock).isSubscribed(AzureService.COMPUTE));
	}
	
	@Test
	public void getVMFolderShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull(new AzureLocation(azureMock).getVMFolder(null));
	}
	
	private Region createRegion(String regionId) {
		Region region = new Region();
		region.setProviderRegionId(regionId);
		region.setName(regionId);
		region.setActive(true);
        region.setAvailable(true);
        region.setJurisdiction("US");
        return region;
	}
	
	private DataCenter createDataCenter(String regionId) {
		DataCenter dataCenter = new DataCenter();
		dataCenter.setActive(true);
		dataCenter.setAvailable(true);
		dataCenter.setName(regionId);
		dataCenter.setProviderDataCenterId(regionId);
		dataCenter.setRegionId(regionId);
        return dataCenter;
	}
	
	private Location createLocation(String name) {
		Location location = new Location();
		location.setName(name);
		location.setDisplayName(name);
		location.setAvailableServices(Arrays.asList("Compute", "Storage", "PersistentVMRole", "HighMemory"));
		return location;
	}
}
