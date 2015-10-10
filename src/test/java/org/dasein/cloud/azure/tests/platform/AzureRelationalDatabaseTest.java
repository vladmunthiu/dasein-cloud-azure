package org.dasein.cloud.azure.tests.platform;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.platform.AzureSqlDatabaseSupport;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServerModel;
import org.dasein.cloud.azure.platform.model.ServerNameModel;
import org.dasein.cloud.azure.platform.model.ServerServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServersModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.azure.tests.HttpMethodAsserts;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseLicenseModel;
import org.dasein.cloud.platform.DatabaseProduct;
import org.dasein.cloud.platform.DatabaseState;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class AzureRelationalDatabaseTest extends AzureTestsBase {

	private final String RESOURCE_SERVERS = "https://management.core.windows.net/%s/services/sqlservers/servers?contentview=generic";
    private final String RESOURCE_SERVERS_NONGEN = "https://management.core.windows.net/%s/services/sqlservers/servers";
    private final String RESOURCE_SERVER = "https://management.core.windows.net/%s/services/sqlservers/servers/%s";
    private final String RESOURCE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases";
    private final String RESOURCE_DATABASE = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases/%s";
    private final String RESOURCE_LIST_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/databases?contentview=generic";
    private final String RESOURCE_SUBSCRIPTION_META = "https://management.core.windows.net/%s/services/sqlservers/subscriptioninfo";
    private final String RESOURCE_LIST_RECOVERABLE_DATABASES = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/recoverabledatabases?contentview=generic";
    private final String RESOURCE_RESTORE_DATABASE_OPERATIONS = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/restoredatabaseoperations";
    private final String RESOURCE_SERVER_FIREWALL = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/firewallrules";
    private final String RESOURCE_FIREWALL_RULE = "https://management.core.windows.net/%s/services/sqlservers/servers/%s/firewallrules/%s";
	
	private final String SERVER_ID = "TEST_SERVER";
	private final String DATABASE_ID = "TEST_DATABASE";
	
	private final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz");
	
	@Rule
    public final TestName name = new TestName();
	
	@Test
	public void getDatabaseShouldReturnCorrectResult() throws CloudException, InternalException, IOException {
		
		final String serverRegion = REGION + "_SERVER";
		
		ServersModel serversModel = new ServersModel();
		serversModel.setServers(Arrays.asList(createServerModel(SERVER_ID, serverRegion, "test", "test")));
		
		final CloseableHttpResponse getServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = createDatabaseServiceResourceModel(DATABASE_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
					return responseHandler.handleResponse(getServersResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(getDatabaseResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
			{ azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		Database expectedResult = createDatabase(
				SERVER_ID, 
				databaseServiceResourceModel.getName(), 
				Integer.parseInt(databaseServiceResourceModel.getMaxSizeGB()), 
				new DateTime(databaseServiceResourceModel.getCreationDate()).getMillis(), 
				databaseServiceResourceModel.getEdition(),
				serverRegion);
		Database actualResult = new AzureSqlDatabaseSupport(azureMock).getDatabase(String.format("%s:%s", SERVER_ID, databaseServiceResourceModel.getName()));
		assertReflectionEquals("match fields for database failed", expectedResult, actualResult);
	}
	
	
	
	@Test(expected = InternalException.class)
	public void getDatabaseShouldThrowExceptionIfDatabaseIdIsNull() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).getDatabase(null);
	}
	
	@Test(expected = InternalException.class)
	public void getDatabaseShouldThrowExceptionIfDatabaseIdFormatIsInvalid() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).getDatabase(DATABASE_ID);
	}
	
	@Test
	public void getDatabaseShouldReturnCorrectResultIfNoServerFound() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(new ServersModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
				return responseHandler.handleResponse(responseMock);
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		assertNull("database found for invalid server", new AzureSqlDatabaseSupport(azureMock).getDatabase(SERVER_ID + ":" + DATABASE_ID));
	}
	
	@Test
	public void addAccessShouldReturnCorrectResult() throws CloudException, InternalException {
		
		final String startIpAddress = "202.100.10.10";
		final String endIpAddress = "202.100.10.100";
		
		ServersModel serversModel = new ServersModel();
		serversModel.setServers(Arrays.asList(createServerModel(SERVER_ID, REGION, "test", "test")));
		
		final CloseableHttpResponse getServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = this.createDatabaseServiceResourceModel(DATABASE_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});
		
		ServerServiceResourceModel serverServiceResourceModel = new ServerServiceResourceModel();
		serverServiceResourceModel.setStartIpAddress(startIpAddress);
		serverServiceResourceModel.setEndIpAddress(endIpAddress);
		serverServiceResourceModel.setName(String.format("%s_%s", databaseServiceResourceModel.getName(), new Date().getTime()));
		
		final CloseableHttpResponse addFilewallResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourceModel>(serverServiceResourceModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
					return responseHandler.handleResponse(getServersResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(getDatabaseResponseMock);
				} else if (inv.getInvocationCount() == 3) {
					assertPost(request, String.format(RESOURCE_SERVER_FIREWALL, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(addFilewallResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		new AzureSqlDatabaseSupport(azureMock).addAccess(String.format("%s:%s", SERVER_ID, DATABASE_ID), String.format("%s::%s", startIpAddress, endIpAddress));
		
	}
	
	@Test
	public void createFromScratchShouldReturnCorrectResult() throws CloudException, InternalException {
		
		ServerNameModel serverNameModel = new ServerNameModel();
		serverNameModel.setName(SERVER_ID);
		
		final CloseableHttpResponse createServerResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerNameModel>(serverNameModel),
				new Header[]{});
		
		ServerServiceResourceModel serverServiceResourceModel = new ServerServiceResourceModel();
		ServerServiceResourceModel.Version versionModel = new ServerServiceResourceModel.Version();
		versionModel.setName("2.0");
		ServerServiceResourceModel.Edition editionModel = new ServerServiceResourceModel.Edition();
		editionModel.setName("Basic");
		ServerServiceResourceModel.ServiceLevelObjective serviceLevelObjectiveModel = new ServerServiceResourceModel.ServiceLevelObjective();
		serviceLevelObjectiveModel.setName("10GB");
		serviceLevelObjectiveModel.setId("ServiceLevelObjectiveModelID");
		editionModel.setServiceLevelObjectives(Arrays.asList(serviceLevelObjectiveModel));
		versionModel.setEditions(Arrays.asList(editionModel));
		serverServiceResourceModel.setVersions(Arrays.asList(versionModel));
		serverServiceResourceModel.setName(SERVER_ID);
		
		final CloseableHttpResponse getSubscriptionVersionProductsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourceModel>(serverServiceResourceModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = new DatabaseServiceResourceModel();
		databaseServiceResourceModel.setName(DATABASE_ID);
		
		final CloseableHttpResponse createDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});

		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertPost(request, String.format(RESOURCE_SERVERS, ACCOUNT_NO));
					return responseHandler.handleResponse(createServerResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_SUBSCRIPTION_META, ACCOUNT_NO));
					return responseHandler.handleResponse(getSubscriptionVersionProductsResponseMock);
				} else if (inv.getInvocationCount() == 3) {
					assertPost(request, String.format(RESOURCE_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(createDatabaseResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		String expectedDatabaseId = String.format("%s:%s", SERVER_ID, DATABASE_ID);
		
		DatabaseProduct product = new DatabaseProduct(serviceLevelObjectiveModel.getName());
		product.setName(editionModel.getName());
		String actualDatabaseId = new AzureSqlDatabaseSupport(azureMock).createFromScratch(DATABASE_ID, product, product.getName(), "test", "test", 3306);
		
		assertEquals("database id not match", expectedDatabaseId, actualDatabaseId);
	}
	
	@Test(expected = InternalException.class)
	public void createFromScratchShouldThrowExceptionIfAdminUserNameIsInvalid() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).createFromScratch(DATABASE_ID, new DatabaseProduct("10GB"), "Basic", "administrator", "administrator", 3306);
	}
	
	@Test(expected = InternalException.class)
	public void createFromScratchShouldThrowExceptionIfProductIsNull() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).createFromScratch(DATABASE_ID, null, "Basic", "new_user", "new_user", 3306);
	}
	
	@Test(expected = CloudException.class)
	public void createFromScratchShouldDeleteServerAndThrowExceptionIfCreateDatabaseFailed() throws CloudException, InternalException {
		
		ServerNameModel serverNameModel = new ServerNameModel();
		serverNameModel.setName(SERVER_ID);
		
		final CloseableHttpResponse createServerResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerNameModel>(serverNameModel),
				new Header[]{});
		
		ServerServiceResourceModel serverServiceResourceModel = new ServerServiceResourceModel();
		ServerServiceResourceModel.Version versionModel = new ServerServiceResourceModel.Version();
		versionModel.setName("2.0");
		ServerServiceResourceModel.Edition editionModel = new ServerServiceResourceModel.Edition();
		editionModel.setName("Basic");
		ServerServiceResourceModel.ServiceLevelObjective serviceLevelObjectiveModel = new ServerServiceResourceModel.ServiceLevelObjective();
		serviceLevelObjectiveModel.setName("10GB");
		serviceLevelObjectiveModel.setId("ServiceLevelObjectiveModelID");
		editionModel.setServiceLevelObjectives(Arrays.asList(serviceLevelObjectiveModel));
		versionModel.setEditions(Arrays.asList(editionModel));
		serverServiceResourceModel.setVersions(Arrays.asList(versionModel));
		serverServiceResourceModel.setName(SERVER_ID);
		
		final CloseableHttpResponse getSubscriptionVersionProductsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourceModel>(serverServiceResourceModel),
				new Header[]{});
		
		final CloseableHttpResponse deleteServerResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertPost(request, String.format(RESOURCE_SERVERS, ACCOUNT_NO));
					return responseHandler.handleResponse(createServerResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_SUBSCRIPTION_META, ACCOUNT_NO));
					return responseHandler.handleResponse(getSubscriptionVersionProductsResponseMock);
				} else if (inv.getInvocationCount() == 3) {
					throw new RuntimeException("Create database failed");
				} else if (inv.getInvocationCount() == 4) {
					assertDelete(request, String.format(RESOURCE_SERVER, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(deleteServerResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		DatabaseProduct product = new DatabaseProduct(serviceLevelObjectiveModel.getName());
		product.setName(editionModel.getName());
		new AzureSqlDatabaseSupport(azureMock).createFromScratch(DATABASE_ID, product, product.getName(), "test", "test", 3306);
	}
	
	@Test(expected = InternalException.class)
	public void addAccessShouldThrowExceptionIfCidrFormatIsInvalid() throws CloudException, InternalException {
		
		ServersModel serversModel = new ServersModel();
		serversModel.setServers(Arrays.asList(createServerModel(SERVER_ID, REGION, "test", "test")));
		
		final CloseableHttpResponse getServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = this.createDatabaseServiceResourceModel(DATABASE_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
					return responseHandler.handleResponse(getServersResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(getDatabaseResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		new AzureSqlDatabaseSupport(azureMock).addAccess(DATABASE_ID, "202.100.10.89/16");
		
	}
	
	@Test(expected = InternalException.class)
	public void addAccessShouldThrowExceptionIfCidrIsNull() throws CloudException, InternalException {
		
		ServersModel serversModel = new ServersModel();
		serversModel.setServers(Arrays.asList(createServerModel(SERVER_ID, REGION, "test", "test")));
		
		final CloseableHttpResponse getServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = this.createDatabaseServiceResourceModel(DATABASE_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});
	
		new MockUp<CloseableHttpClient>() {
			@Mock
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
					return responseHandler.handleResponse(getServersResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(getDatabaseResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		new AzureSqlDatabaseSupport(azureMock).addAccess(DATABASE_ID, null);
	}
	
	@Test
	public void getDatabaseEnginesShouldReturnCorrectResult() throws CloudException, InternalException {
		assertReflectionEquals("match database engines failed", 
				Arrays.asList(DatabaseEngine.SQLSERVER_EE), 
				new AzureSqlDatabaseSupport(azureMock).getDatabaseEngines());
	}
	
	@Test
	public void getDefaultVersionShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals("match default version failed", "2.0", new AzureSqlDatabaseSupport(azureMock).getDefaultVersion(null));
	}
	
	@Test
	public void getSupportedVersionsShouldReturnCorrectResult() throws AssertionFailedError, CloudException, InternalException {
		assertReflectionEquals("match supported versions failed", 
				Arrays.asList("2.0"),
				new AzureSqlDatabaseSupport(azureMock).getSupportedVersions(null));
	}
	
	@Test
	public void listDatabaseProductsShouldReturnCorrectResult() throws CloudException, InternalException {
		
		ServerServiceResourceModel serverServiceResourceModel = new ServerServiceResourceModel();
		ServerServiceResourceModel.Version versionModel = new ServerServiceResourceModel.Version();
		versionModel.setName("2.0");
		ServerServiceResourceModel.Edition editionModel = new ServerServiceResourceModel.Edition();
		editionModel.setName("Basic");
		ServerServiceResourceModel.ServiceLevelObjective serviceLevelObjectiveModel = new ServerServiceResourceModel.ServiceLevelObjective();
		serviceLevelObjectiveModel.setName("10GB");
		serviceLevelObjectiveModel.setId("ServiceLevelObjectiveModelID");
		editionModel.setServiceLevelObjectives(Arrays.asList(serviceLevelObjectiveModel));
		versionModel.setEditions(Arrays.asList(editionModel));
		serverServiceResourceModel.setVersions(Arrays.asList(versionModel));
		serverServiceResourceModel.setName(SERVER_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourceModel>(serverServiceResourceModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SUBSCRIPTION_META, ACCOUNT_NO));
				return responseHandler.handleResponse(getDatabaseResponseMock);
			}
		};
		
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
		
		ArrayList<DatabaseProduct> expectedDatabaseProducts = new ArrayList<DatabaseProduct>();
		
		DatabaseProduct expectedDatabaseProduct = new DatabaseProduct(serviceLevelObjectiveModel.getName(), editionModel.getName());
		expectedDatabaseProduct.setEngine(DatabaseEngine.SQLSERVER_EE);
		expectedDatabaseProduct.setLicenseModel(DatabaseLicenseModel.LICENSE_INCLUDED);
		expectedDatabaseProducts.add(expectedDatabaseProduct);
		
		assertReflectionEquals("match fields failed", 
				expectedDatabaseProducts, 
				new AzureSqlDatabaseSupport(azureMock).listDatabaseProducts(DatabaseEngine.SQLSERVER_EE));
	}
	
	@Test
	public void listDatabaseProductsShouldReturnCorrectResultForSomeEngine() throws AssertionFailedError, CloudException, InternalException {
		assertReflectionEquals("match database products failed", 
				Arrays.asList(), 
				new AzureSqlDatabaseSupport(azureMock).listDatabaseProducts(DatabaseEngine.MYSQL));
	}
	
	private DatabaseServiceResourceModel createDatabaseServiceResourceModel(String databaseId) {
		DatabaseServiceResourceModel databaseServiceResourceModel = new DatabaseServiceResourceModel();
		databaseServiceResourceModel.setName(databaseId);
		databaseServiceResourceModel.setMaxSizeGB("100");
		databaseServiceResourceModel.setCreationDate(format.print(new DateTime().getMillis()));
		databaseServiceResourceModel.setState("Normal");
		databaseServiceResourceModel.setEdition("Basic");
		return databaseServiceResourceModel;
	}
	
	private ServerModel createServerModel(String serverId, String serverRegion, String admin, String pwd) {
		ServerModel serverModel = new ServerModel();
		serverModel.setName(serverId);
		serverModel.setLocation(serverRegion);
		serverModel.setAdministratorLogin(admin);
		serverModel.setAdministratorLoginPassword(pwd);
		return serverModel;
	}
	
	private Database createDatabase(String serverId, String databaseId, Integer maxSizeGB, long creationTimestamp, String edition, String serverRegion) {
		Database database = new Database();
		database.setName(databaseId);
		database.setProviderDatabaseId(String.format("%s:%s", serverId, databaseId));
		database.setProviderRegionId(serverRegion);
		database.setProviderOwnerId(ACCOUNT_NO);
		database.setAllocatedStorageInGb(maxSizeGB);
		database.setEngine(DatabaseEngine.SQLSERVER_EE);
		database.setCreationTimestamp(creationTimestamp);
		database.setCurrentState(DatabaseState.AVAILABLE);
		database.setProductSize(edition);
		database.setHostName(String.format("%s.database.windows.net", serverId));
		database.setHostPort(1433);
		return database;
	}
	
}
