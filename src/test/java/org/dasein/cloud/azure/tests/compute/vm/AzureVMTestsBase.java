package org.dasein.cloud.azure.tests.compute.vm;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.AzureSSLSocketFactory;
import org.dasein.cloud.azure.AzureX509;
import org.junit.Before;

/**
 * Created by vmunthiu on 9/7/2015.
 */
public class AzureVMTestsBase {
    @Mocked
    ProviderContext providerContextMock;
    @Mocked
    Azure azureMock;
    @Mocked
    AzureLocation azureLocationMock;
    @Mocked
    AzureSSLSocketFactory azureSSLSocketFactoryMock;
    @Mocked
    AzureX509 azureX509Mock;

    protected final String ACCOUNT_NO = "TESTACCOUNTNO";
    protected final String REGION = "TESTREGION";
    protected final String SERVICE_NAME = "TESTSERVICENAME";
    protected final String DEPLOYMENT_NAME = "TESTDEPLOYMENTNAME";
    protected final String ROLE_NAME = "TESTROLENAME";
    protected final String ENDPOINT = "TESTENDPOINT";
    protected final String VM_NAME = "TESTVMNAME";
    protected final String VM_ID = String.format("%s:%s:%s", SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

    @Before
    public void setUp() {
        new NonStrictExpectations() {
            { azureMock.getContext(); result = providerContextMock; }
            { azureMock.getDataCenterServices(); result = azureLocationMock; }
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };
    }
}
