package org.dasein.cloud.azure.tests.platform;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.platform.AzureSqlDatabaseSupport;
import org.dasein.cloud.azure.platform.model.DatabaseServiceResourceModel;
import org.dasein.cloud.azure.platform.model.ServerModel;
import org.dasein.cloud.azure.platform.model.ServersModel;
import org.dasein.cloud.azure.tests.AzureTestsBase;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseState;
import org.dasein.cloud.util.requester.DaseinResponseHandler;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.cloud.util.requester.streamprocessors.XmlStreamToObjectProcessor;
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
	public void getDatabaseShouldReturnCorrectResult(@Mocked final HttpClientBuilder httpClientBuilder) throws CloudException, InternalException, IOException {
		
		final String serverRegion = REGION + "_SERVER";
		
		ServersModel serversModel = new ServersModel();
		ServerModel serverModel = new ServerModel();
		serverModel.setName(SERVER_ID);
		serverModel.setLocation(serverRegion);
		serverModel.setAdministratorLogin("TESTLOGIN");
		serverModel.setAdministratorLoginPassword("TESTPWD");
		serversModel.setServers(Arrays.asList(serverModel));
		
		final CloseableHttpResponse getServersResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		DatabaseServiceResourceModel databaseServiceResourceModel = new DatabaseServiceResourceModel();
		databaseServiceResourceModel.setName(DATABASE_ID);
		databaseServiceResourceModel.setMaxSizeGB("100");
		databaseServiceResourceModel.setCreationDate(format.print(new DateTime().getMillis()));
		databaseServiceResourceModel.setState("Normal");
		databaseServiceResourceModel.setEdition("Basic");
		
		final CloseableHttpResponse getDatabaseResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(serversModel),
				new Header[]{});
		
		final CloseableHttpClient closeableHttpClient = new MockUp<CloseableHttpClient>() {
			@Mock(invocations = 2)
			<T> T execute(Invocation inv, HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
				if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
            		return (T) responseHandler.handleResponse(getServersResponseMock);
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(RESOURCE_DATABASE, ACCOUNT_NO, SERVER_ID, DATABASE_ID));
            		return (T) responseHandler.handleResponse(getDatabaseResponseMock);
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }    
		}.getMockInstance();
		
		new NonStrictExpectations() {
			{httpClientBuilder.build(); result = closeableHttpClient; }
		};
		
		Database expectedResult = createDatabase(
				serverModel.getName(), 
				databaseServiceResourceModel.getName(), 
				Integer.parseInt(databaseServiceResourceModel.getMaxSizeGB()), 
				new DateTime(databaseServiceResourceModel.getCreationDate()).getMillis(), 
				databaseServiceResourceModel.getEdition(),
				serverRegion);
		Database actualResult = new AzureSqlDatabaseSupport(azureMock).getDatabase(String.format("%s:%s", serverModel.getName(), databaseServiceResourceModel.getName()));
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
	
	public void getDatabaseShouldReturnCorrectResultIfNoServerFound() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ServersModel>(null),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(RESOURCE_SERVERS_NONGEN, ACCOUNT_NO));
        		return responseMock;
            }
		};
		
		assertNull("database found for invalid server", new AzureSqlDatabaseSupport(azureMock).getDatabase(SERVER_ID + ":" + DATABASE_ID));
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
