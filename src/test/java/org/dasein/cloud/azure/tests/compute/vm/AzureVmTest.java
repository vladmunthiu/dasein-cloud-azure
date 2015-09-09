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

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.cloud.util.requester.streamprocessors.XmlStreamToObjectProcessor;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by Vlad_Munthiu on 6/6/2014.
 */
public class AzureVmTest extends AzureVMTestsBase {


    @Test
    public void startShouldPostCorrectRequest()throws CloudException, InternalException{
        final VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.addTag("serviceName", SERVICE_NAME);
        virtualMachine.addTag("deploymentName", DEPLOYMENT_NAME);
        virtualMachine.addTag("roleName", ROLE_NAME);
        virtualMachine.setProviderVirtualMachineId(VM_ID);

        final CloseableHttpResponse mockedHttpResponse = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null, new Header[]{});
        final String expectedUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roleInstances/%s/Operations", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
                assertEquals("Start method shoould do a POST", "POST", request.getMethod());
                assertEquals("Start method does not post to the correct url", expectedUrl, request.getURI().toString());

                return mockedHttpResponse;
            }
        };

        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, virtualMachine);
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
        final VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.addTag("serviceName", SERVICE_NAME);
        virtualMachine.addTag("deploymentName", DEPLOYMENT_NAME);
        virtualMachine.addTag("roleName", ROLE_NAME);
        virtualMachine.setProviderVirtualMachineId(VM_ID);

        PersistentVMRoleModel persistentVMRoleModel = new PersistentVMRoleModel();
        persistentVMRoleModel.setRoleName(VM_NAME);
        persistentVMRoleModel.setOsVersion("OSVERSION");
        persistentVMRoleModel.setRoleType("ROLETYPES");
        persistentVMRoleModel.setRoleSize("OLDSIZE");

        DaseinObjectToXmlEntity<PersistentVMRoleModel> daseinEntity = new DaseinObjectToXmlEntity<PersistentVMRoleModel>(persistentVMRoleModel);
        final CloseableHttpResponse getHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), daseinEntity , new Header[]{});
        final CloseableHttpResponse putHttpResponseMock = getHttpResponseMock(getStatusLineMock(HttpServletResponse.SC_OK), null , new Header[]{});
        final String expectedUrl = String.format("%s/%s/services/hostedservices/%s/deployments/%s/roles/%s", ENDPOINT, ACCOUNT_NO, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);


        new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) throws IOException {
                if(inv.getInvocationCount() == 1) {
                    assertEquals("Start method shoould do a GET", "GET", request.getMethod());
                    assertEquals("Start method should do a GET to the correct url", expectedUrl, request.getURI().toString());

                    return getHttpResponseMock;
                } else {
                    assertEquals("Start method should do a PUT", "PUT", request.getMethod());
                    assertEquals("Start method should do a PUT to the correct url", expectedUrl, request.getURI().toString());
                    PersistentVMRoleModel putObject = new XmlStreamToObjectProcessor<PersistentVMRoleModel>().read(((HttpPut)request).getEntity().getContent(), PersistentVMRoleModel.class);
                    return putHttpResponseMock;
                }

            }
        };

        AzureVMSupport azureVMSupport = new AzureVMSupport(azureMock, virtualMachine);
        azureVMSupport.alterVirtualMachineProduct(VM_ID, "Small");

    }
}
