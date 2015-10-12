package org.dasein.cloud.azure.tests.platform;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.NonStrictExpectations;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.platform.AzureSqlDatabaseSupport;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourceModel;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourcesModel;
import org.dasein.cloud.azure.platform.model.RecoverableDatabaseModel;
import org.dasein.cloud.azure.platform.model.RecoverableDatabasesModel;
import org.dasein.cloud.azure.platform.model.ServerModel;
import org.dasein.cloud.azure.platform.model.ServerNameModel;
import org.dasein.cloud.azure.platform.model.ServerServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServerServiceResourcesModel;
import org.dasein.cloud.azure.platform.model.ServersModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseBackup;
import org.dasein.cloud.platform.DatabaseBackupState;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseLicenseModel;
import org.dasein.cloud.platform.DatabaseProduct;
import org.dasein.cloud.platform.DatabaseState;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
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
	
	private final String SERVER_ID = "TESTSERVER";
	private final String DATABASE_ID = "TESTDATABASE";
	
	private final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	@Rule
    public final TestName name = new TestName();

	@Before
	public void initialize() throws CloudException {
		new NonStrictExpectations() {
		    { azureMock.getAzureClientBuilder(); result = HttpClientBuilder.create(); }
		};
	}
	
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
			@Mock(invocations = 2)
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
	
	@Test
	public void listAccessShouldReturnCorrectResult() throws CloudException, InternalException {
		
		ServerServiceResourcesModel serverServiceResourcesModel = new ServerServiceResourcesModel();
		serverServiceResourcesModel.setServerServiceResourcesModels(Arrays.asList(
				createServerServiceResourceModel("TESTFIREWALLRULE", "202.100.10.10", "202.100.10.100")));
		
		final CloseableHttpResponse listFirewallRulesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourcesModel>(serverServiceResourcesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVER_FIREWALL, ACCOUNT_NO, SERVER_ID));
				return responseHandler.handleResponse(listFirewallRulesResponseMock);
			}
		};
		
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		assertReflectionEquals("match fields of access for database failed", 
				Arrays.asList(String.format("%s::%s::%s", "TESTFIREWALLRULE", "202.100.10.10", "202.100.10.100")),
				new AzureRelationalDatabaseSupport(azureMock, database).listAccess(DATABASE_ID));
	}
	
	@Test(expected = InternalException.class)
	public void listAccessShouldThrowExceptionIfNoDatabaseFound() throws CloudException, InternalException {
		new AzureRelationalDatabaseSupport(azureMock, null).listAccess(DATABASE_ID);
	}
	
	@Test
	public void listAccessShouldReturnCorrectResultIfNoFirewallRulesFound() throws CloudException, InternalException {
		
		final CloseableHttpResponse listFirewallRulesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourcesModel>(new ServerServiceResourcesModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVER_FIREWALL, ACCOUNT_NO, SERVER_ID));
				return responseHandler.handleResponse(listFirewallRulesResponseMock);
			}
		};
		
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		assertReflectionEquals("match fields of access for database failed", 
				new ArrayList<String>(), 
				new AzureRelationalDatabaseSupport(azureMock, database).listAccess(DATABASE_ID));
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
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
				return responseHandler.handleResponse(responseMock);
			}
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
		
		DatabaseServiceResourceModel databaseServiceResourceModel = createDatabaseServiceResourceModel(DATABASE_ID);
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourceModel>(databaseServiceResourceModel),
				new Header[]{});
		
		final CloseableHttpResponse addFilewallResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourceModel>(
						createServerServiceResourceModel(String.format("%s_%s", databaseServiceResourceModel.getName(), new Date().getTime()), 
								startIpAddress, endIpAddress)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 3)
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
		
		new AzureSqlDatabaseSupport(azureMock).addAccess(String.format("%s:%s", SERVER_ID, DATABASE_ID), String.format("%s::%s", startIpAddress, endIpAddress));
	}
	
	@Test(expected = InternalException.class)
	public void addAccessShouldThrowExceptionIfCidrFormatIsInvalid() throws CloudException, InternalException {
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		new AzureRelationalDatabaseSupport(azureMock, database).addAccess(DATABASE_ID, "202.100.10.89/16");
		
	}
	
	@Test(expected = InternalException.class)
	public void addAccessShouldThrowExceptionIfCidrIsNull() throws CloudException, InternalException {
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		new AzureRelationalDatabaseSupport(azureMock, database).addAccess(DATABASE_ID, null);
	}
	
	@Test(expected = InternalException.class)
	public void addAccessShouldThrowExceptionIfNoDatabaseFound() throws CloudException, InternalException {
		final String startIpAddress = "202.100.10.10";
		final String endIpAddress = "202.100.10.100";
		new AzureRelationalDatabaseSupport(azureMock, null).addAccess(DATABASE_ID, String.format("%s::%s", startIpAddress, endIpAddress));
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
			@Mock(invocations = 3)
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
			@Mock(invocations = 4)
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
		
		DatabaseProduct product = new DatabaseProduct(serviceLevelObjectiveModel.getName());
		product.setName(editionModel.getName());
		new AzureSqlDatabaseSupport(azureMock).createFromScratch(DATABASE_ID, product, product.getName(), "test", "test", 3306);
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
	
	@Test
	public void createFromLatestShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).createFromLatest(DATABASE_ID, DATABASE_ID, "10GB", REGION, 3306));
	}
	
	@Test
	public void createFromSnapshotShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).createFromSnapshot(
				DATABASE_ID, DATABASE_ID, DATABASE_ID + "_SNAPSHOT", "10GB", REGION, 3306));
	}
	
	@Test
	public void createFromTimestampShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).createFromTimestamp(
				DATABASE_ID, DATABASE_ID, new Date().getTime(), "10GB", REGION, 3306));
	}
	
	@Test
	public void getConfigurationShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).getConfiguration(null));
	}
	
	@Test
	public void getSnapshotShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).getSnapshot(null));
	}
	
	@Test
	public void listConfigurationsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).listConfigurations());
	}
	
	@Test
	public void listDatabaseStatusShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).listDatabaseStatus());
	}
	
	@Test
	public void listDatabasesShouldReturnCorrectResult() throws CloudException, InternalException {
	
		ServersModel serversModel = new ServersModel();
		serversModel.setServers(Arrays.asList(
				createServerModel(SERVER_ID, REGION, "test", "test"),
				createServerModel(SERVER_ID, REGION + "_INVALID", "test", "test")));
		
		final CloseableHttpResponse listServerNonGenResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel1 = createDatabaseServiceResourceModel(DATABASE_ID + "_1");
		DatabaseServiceResourceModel databaseServiceResourceModel2 = createDatabaseServiceResourceModel(DATABASE_ID + "_2");
		DatabaseServiceResourceModel databaseServiceResourceModel3 = createDatabaseServiceResourceModel("master");
		
		DatabaseServiceResourcesModel databaseServiceResourcesModel = new DatabaseServiceResourcesModel();
		databaseServiceResourcesModel.setDatabaseServiceResourceModels(Arrays.asList(
				databaseServiceResourceModel1, databaseServiceResourceModel2, databaseServiceResourceModel3));
		
		final CloseableHttpResponse listDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourcesModel>(databaseServiceResourcesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 2)
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
					return responseHandler.handleResponse(listServerNonGenResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_LIST_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(listDatabasesResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		assertReflectionEquals("match fields for databases failed", 
				Arrays.asList(
						this.createDatabase(SERVER_ID, databaseServiceResourceModel1.getName(), Integer.valueOf(databaseServiceResourceModel1.getMaxSizeGB()), 
							format.parseDateTime(databaseServiceResourceModel1.getCreationDate()).getMillis(), databaseServiceResourceModel1.getEdition(), REGION),
						this.createDatabase(SERVER_ID, databaseServiceResourceModel2.getName(), Integer.valueOf(databaseServiceResourceModel2.getMaxSizeGB()), 
							format.parseDateTime(databaseServiceResourceModel2.getCreationDate()).getMillis(), databaseServiceResourceModel2.getEdition(), REGION)),
						new AzureSqlDatabaseSupport(azureMock).listDatabases());
	}
	
	@Test
	public void listDatabasesShouldReturnCorrectResultIfNoServerFound() throws AssertionFailedError, CloudException, InternalException {
		
		final CloseableHttpResponse listServerNonGenResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(new ServersModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
				return responseHandler.handleResponse(listServerNonGenResponseMock);
			}
		};
		
		assertReflectionEquals("match fields for databases failed", 
				Arrays.asList(), new AzureSqlDatabaseSupport(azureMock).listDatabases());
	}
	
	@Test
	public void listParametersShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).listParameters(null));
	}
	
	@Test
	public void listSnapshotsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertNull(new AzureSqlDatabaseSupport(azureMock).listSnapshots(null));
	}

	@Test
	public void removeDatabaseShouldDeleteWithCorrectRequestIfExistSingleDatabase() throws CloudException, InternalException {
		
		final CloseableHttpResponse deleteResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		final CloseableHttpResponse listDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourcesModel>(new DatabaseServiceResourcesModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 3)
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertDelete(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(deleteResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_LIST_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(listDatabasesResponseMock);
				} else if (inv.getInvocationCount() == 3) {
					assertDelete(request, String.format(RESOURCE_SERVER, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(deleteResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new AzureSqlDatabaseSupport(azureMock).removeDatabase(String.format("%s:%s", SERVER_ID, DATABASE_ID));
	}
	
	@Test
	public void removeDatabaseShouldDeleteWithCorrectRequestIfExistSingleAndMasterDatabase() throws CloudException, InternalException {
		
		final CloseableHttpResponse deleteResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		DatabaseServiceResourcesModel databaseServiceResourcesModel = new DatabaseServiceResourcesModel();
		databaseServiceResourcesModel.setDatabaseServiceResourceModels(Arrays.asList(createDatabaseServiceResourceModel("master")));
		
		final CloseableHttpResponse listDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourcesModel>(databaseServiceResourcesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 3)
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertDelete(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(deleteResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_LIST_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(listDatabasesResponseMock);
				} else if (inv.getInvocationCount() == 3) {
					assertDelete(request, String.format(RESOURCE_SERVER, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(deleteResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new AzureSqlDatabaseSupport(azureMock).removeDatabase(String.format("%s:%s", SERVER_ID, DATABASE_ID));
	}
	
	@Test
	public void removeDatabaseShouldDeleteWithCorrectRequestIfExistMultipleDatabases() throws CloudException, InternalException {
		
		final CloseableHttpResponse deleteResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		DatabaseServiceResourcesModel databaseServiceResourcesModel = new DatabaseServiceResourcesModel();
		databaseServiceResourcesModel.setDatabaseServiceResourceModels(Arrays.asList(
				createDatabaseServiceResourceModel("master"),
				createDatabaseServiceResourceModel(DATABASE_ID + "_OTHERS")));
		
		final CloseableHttpResponse listDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<DatabaseServiceResourcesModel>(databaseServiceResourcesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 2)
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertDelete(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
					return responseHandler.handleResponse(deleteResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_LIST_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(listDatabasesResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		new AzureSqlDatabaseSupport(azureMock).removeDatabase(String.format("%s:%s", SERVER_ID, DATABASE_ID));
	}
	
	@Test(expected = InternalException.class)
	public void removeDatabaseShouldThrowExceptionIfDatabaseIdIsNull() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).removeDatabase(null);
	}
	
	@Test(expected = InternalException.class)
	public void removeDatabaseShouldThrowExceptionIfDatabaseIdFormatIsInvalid() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).removeDatabase(DATABASE_ID);
	}
	
	@Test
	public void revokeAccessShouldDeleteWithCorrectRequest() throws CloudException, InternalException {
		
		final String ruleName = "TESTFIREWALLRULE";
		final String startIpAddress = "202.100.10.10";
		final String endIpAddress = "202.100.10.100";
		
		final CloseableHttpResponse deleteResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertDelete(request, String.format(RESOURCE_FIREWALL_RULE, ACCOUNT_NO, SERVER_ID, ruleName));
				return responseHandler.handleResponse(deleteResponseMock);
			}
		};
		
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		new AzureRelationalDatabaseSupport(azureMock, database).revokeAccess(String.format("%s:%s", SERVER_ID, DATABASE_ID), 
				String.format("%s::%s::%s", ruleName, startIpAddress, endIpAddress));
	}
	
	@Test(expected = InternalException.class)
	public void revokeAccessShouldThrowExceptionIfNoDatabaseFound() throws CloudException, InternalException {
		final String ruleName = "TESTFIREWALLRULE";
		final String startIpAddress = "202.100.10.10";
		final String endIpAddress = "202.100.10.100";
		new AzureRelationalDatabaseSupport(azureMock, null).revokeAccess(String.format("%s:%s", SERVER_ID, DATABASE_ID), 
				String.format("%s::%s::%s", ruleName, startIpAddress, endIpAddress));
	}
	
	@Test(expected = InternalError.class)
	public void revokeAccessShouldThrowExceptionIfCidrFormatIsInvalid() throws CloudException, InternalException {
		Database database = createDatabase(SERVER_ID, DATABASE_ID, 10, new Date().getTime(), "Basic", REGION);
		new AzureRelationalDatabaseSupport(azureMock, database).revokeAccess(String.format("%s:%s", SERVER_ID, DATABASE_ID), "TESTFIREWALLRULE");
	}
	
	@Test
	public void snapshotShouldReturnCorrectResult() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).snapshot(null, null);
	}
	
	@Test
	public void getUsableBackupShouldReturnCorrectResult() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).getUsableBackup(null, null);
	}
	
	@Test
	public void listBackupsForDatabaseShouldReturnCorrectResult() throws CloudException, InternalException {
		
		final String databaseBackupName = DATABASE_ID + "BACKUP";

		RecoverableDatabasesModel recoverableDatabasesModel = new RecoverableDatabasesModel();
		RecoverableDatabaseModel recoverableDatabaseModel1 = new RecoverableDatabaseModel();
		recoverableDatabaseModel1.setName("AutomatedSqlExport_" + DATABASE_ID + "BACKUP1" + "_20150114T100004Z");
		RecoverableDatabaseModel recoverableDatabaseModel2 = new RecoverableDatabaseModel();
		recoverableDatabaseModel2.setName(databaseBackupName);
		recoverableDatabasesModel.setRecoverableDatabaseModels(Arrays.asList(recoverableDatabaseModel1, recoverableDatabaseModel2));
		
		final CloseableHttpResponse getRecoverableDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<RecoverableDatabasesModel>(recoverableDatabasesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_LIST_RECOVERABLE_DATABASES, ACCOUNT_NO, SERVER_ID));
				return responseHandler.handleResponse(getRecoverableDatabasesResponseMock);
			}
		};
		
		assertReflectionEquals("match fields for backup databases failed", 
				Arrays.asList(createDatabaseBackup(databaseBackupName, databaseBackupName)), 
						new AzureSqlDatabaseSupport(azureMock).listBackups(String.format("%s:%s", SERVER_ID, databaseBackupName)));
	}
	
	@Test(expected = InternalException.class)
	public void listBackupsForDatabaseShouldThrowExceptionIfDatabaseIdFormatIsInvalid() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).listBackups(DATABASE_ID);
	}
	
	@Test
	public void listAllBackupsShouldReturnCorrectResult() throws CloudException, InternalException {
		 
		final String startIpAddress = "202.100.10.10";
		final String endIpAddress = "202.100.10.100";
		
		ServerServiceResourcesModel serverServiceResourcesModel = new ServerServiceResourcesModel();
		serverServiceResourcesModel.setServerServiceResourcesModels(Arrays.asList(
				createServerServiceResourceModel(SERVER_ID, startIpAddress, endIpAddress)));
		
		final CloseableHttpResponse listServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourcesModel>(serverServiceResourcesModel),
				new Header[]{});
		
		RecoverableDatabasesModel recoverableDatabasesModel = new RecoverableDatabasesModel();
		RecoverableDatabaseModel recoverableDatabaseModel1 = new RecoverableDatabaseModel();
		recoverableDatabaseModel1.setName("AutomatedSqlExport_" + DATABASE_ID + "BACKUP1" + "_20150114T100004Z");
		RecoverableDatabaseModel recoverableDatabaseModel2 = new RecoverableDatabaseModel();
		recoverableDatabaseModel2.setName(DATABASE_ID + "BACKUP2");
		recoverableDatabasesModel.setRecoverableDatabaseModels(Arrays.asList(recoverableDatabaseModel1, recoverableDatabaseModel2));
		
		final CloseableHttpResponse getRecoverableDatabasesResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<RecoverableDatabasesModel>(recoverableDatabasesModel),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 2)
			public <T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				if (inv.getInvocationCount() == 1) {
					assertGet(request, String.format(RESOURCE_SERVERS, ACCOUNT_NO));
					return responseHandler.handleResponse(listServersResponseMock);
				} else if (inv.getInvocationCount() == 2) {
					assertGet(request, String.format(RESOURCE_LIST_RECOVERABLE_DATABASES, ACCOUNT_NO, SERVER_ID));
					return responseHandler.handleResponse(getRecoverableDatabasesResponseMock);
				} else {
					throw new RuntimeException("Invalid invocation count!");
				}
			}
		};
		
		assertReflectionEquals("match fields for backup databases failed", 
				Arrays.asList(
						createDatabaseBackup(DATABASE_ID + "BACKUP1", "AutomatedSqlExport_" + DATABASE_ID + "BACKUP1" + "_20150114T100004Z"),
						createDatabaseBackup(DATABASE_ID + "BACKUP2", DATABASE_ID + "BACKUP2")), 
				new AzureSqlDatabaseSupport(azureMock).listBackups(null));
	}
	
	@Test
	public void listAllBackupsShouldReturnCorrectResultIfNoServerFound() throws CloudException, InternalException {
		
		final CloseableHttpResponse listServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServerServiceResourcesModel>(new ServerServiceResourcesModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertGet(request, String.format(RESOURCE_SERVERS, ACCOUNT_NO));
				return responseHandler.handleResponse(listServersResponseMock);
			}
		};
		
		assertReflectionEquals(Arrays.asList(), new AzureSqlDatabaseSupport(azureMock).listBackups(null));
	}
	
	@Test
	public void createFromBackupShouldPostWithCorrectRequest() throws CloudException, InternalException {
		
		DatabaseBackup databaseBackup = this.createDatabaseBackup(DATABASE_ID + "BACKUP", DATABASE_ID + "BACKUP");
		
		final CloseableHttpResponse createDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 1)
			public <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) throws IOException {
				assertPost(request, String.format(RESOURCE_RESTORE_DATABASE_OPERATIONS, ACCOUNT_NO, SERVER_ID));
				return responseHandler.handleResponse(createDatabaseResponseMock);
			}
		};
		
		new AzureSqlDatabaseSupport(azureMock).createFromBackup(databaseBackup, DATABASE_ID);
	}
	
	@Test(expected = InternalException.class)
	public void createFromBackupShouldThrowExceptionIfBackupIsNull() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).createFromBackup(null, DATABASE_ID);
	}
	
	@Test(expected = InternalException.class)
	public void createFromBackupShouldThrowExceptionIfBackupDatabaseNameIsNull() throws CloudException, InternalException {
		new AzureSqlDatabaseSupport(azureMock).createFromBackup(new DatabaseBackup(), DATABASE_ID);
	}
	
	@Test(expected = InternalException.class)
	public void createFromBackupShouldThrowExceptionIfBackupDatabaseNameFormatIsInvalid() throws CloudException, InternalException {
		DatabaseBackup databaseBackup = new DatabaseBackup();
		databaseBackup.setProviderDatabaseId(DATABASE_ID);
		new AzureSqlDatabaseSupport(azureMock).createFromBackup(databaseBackup, DATABASE_ID);
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
	
	private ServerServiceResourceModel createServerServiceResourceModel(String name, String startIpAddress, String endIpAddress) {
		ServerServiceResourceModel serverServiceResourceModel = new ServerServiceResourceModel();
		serverServiceResourceModel.setName(name);
		serverServiceResourceModel.setStartIpAddress(startIpAddress);
		serverServiceResourceModel.setEndIpAddress(endIpAddress);
		return serverServiceResourceModel;
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
	
	private DatabaseBackup createDatabaseBackup(String databaseName, String backupName) {
		DatabaseBackup databaseBackup = new DatabaseBackup();
        databaseBackup.setProviderDatabaseId(String.format("%s:%s", SERVER_ID, databaseName));
        databaseBackup.setProviderOwnerId(ACCOUNT_NO);
        databaseBackup.setProviderRegionId(REGION);
        databaseBackup.setCurrentState(DatabaseBackupState.AVAILABLE);
        databaseBackup.setProviderBackupId(backupName);
        return databaseBackup;
	}
	
}
