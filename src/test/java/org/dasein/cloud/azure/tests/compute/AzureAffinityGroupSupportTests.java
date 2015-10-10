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

package org.dasein.cloud.azure.tests.compute;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.AzureAffinityGroupSupport;
import org.dasein.cloud.azure.compute.model.AffinityGroupModel;
import org.dasein.cloud.azure.compute.model.AffinityGroupModel.ComputeCapabilities;
import org.dasein.cloud.azure.compute.model.AffinityGroupModel.HostedService;
import org.dasein.cloud.azure.compute.model.AffinityGroupsModel;
import org.dasein.cloud.azure.compute.model.UpdateAffinityGroupModel;
import org.dasein.cloud.azure.tests.AzureTestsBaseWithLocation;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupCreateOptions;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.junit.Test;

import mockit.*;
import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;


/**
 * Created by Vlad_Munthiu on 7/23/2014.
 */
public class AzureAffinityGroupSupportTests extends AzureTestsBaseWithLocation {
	
	private final String RESOURCE_AFFINITYGROUPS = "%s/%s/affinitygroups";
    private final String RESOURCE_AFFINITYGROUP = "%s/%s/affinitygroups/%s";
	
	private final String AFFINITY_GROUP_ID = "TESTAFFINITYGROUP";
	
	@Test
	public void createShouldPostWithCorrectRequest() throws InternalException, CloudException {
		
		AffinityGroup expectedResult = AffinityGroup.getInstance(
				AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, REGION, null);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertPost(request, String.format(RESOURCE_AFFINITYGROUPS, ENDPOINT, ACCOUNT_NO));
            	return responseMock;
            }
		};
		
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, REGION);
		AffinityGroup actualResult = new AzureAffinityGroupSupport(azureMock).create(options);
		assertReflectionEquals("match fields for affinity group failed", expectedResult, actualResult);
	}
	
	@Test(expected=InternalException.class)
	public void createShouldThrowExceptionIfOptionsIsInsufficient() throws InternalException, CloudException {
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(null, null, REGION);
		new AzureAffinityGroupSupport(azureMock).create(options);
	}
	
	@Test(expected=InternalException.class)
	public void createShouldThrowExceptionIfParsingRequestEntityFailed() throws InternalException, CloudException {
		new MockUp<AzureMethod>() {
			@Mock
			<T> String post(String resource, T object) throws JAXBException, CloudException, InternalException {
				throw new JAXBException("parsing object failed");
			}
		};
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, REGION);
		new AzureAffinityGroupSupport(azureMock).create(options);
	}
	
	@Test
	public void deleteShouldDeleteWithCorrectRequest() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertDelete(request, String.format(RESOURCE_AFFINITYGROUP, ENDPOINT, ACCOUNT_NO, AFFINITY_GROUP_ID));
            	return responseMock;
            }
		};
		
		new AzureAffinityGroupSupport(azureMock).delete(AFFINITY_GROUP_ID);
	}
	
	@Test(expected=InternalException.class)
	public void deleteShouldThrowExceptionIfIdIsNull() throws InternalException, CloudException {
		new AzureAffinityGroupSupport(azureMock).delete(null);
	}
	
	@Test(expected=InternalException.class)
	public void deleteShouldThrowExceptionIfIdIsEmpty() throws InternalException, CloudException {
		new AzureAffinityGroupSupport(azureMock).delete("");
	}
	
	@Test
	public void getShouldReturnCorrectResult() throws InternalException, CloudException {
		
		AffinityGroupModel model = createAffinityGroupModel(AFFINITY_GROUP_ID, REGION, AFFINITY_GROUP_ID);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<AffinityGroupModel>(model),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(RESOURCE_AFFINITYGROUP, ENDPOINT, ACCOUNT_NO, AFFINITY_GROUP_ID));
            	return responseMock;
            }
		};
		
		AffinityGroup expectedResult = AffinityGroup.getInstance(
				model.getName(),model.getName(),model.getDescription(), model.getLocation(), null);
		AffinityGroup actualResult = new AzureAffinityGroupSupport(azureMock).get(AFFINITY_GROUP_ID);
		assertReflectionEquals("match fields for affinity group failed", expectedResult, actualResult);
	}
	
	@Test(expected=InternalException.class)
	public void getShouldThrowExceptionIfIdIsNull() throws InternalException, CloudException {
		new AzureAffinityGroupSupport(azureMock).get(null);
	}
	
	@Test(expected=InternalException.class)
	public void getShouldThrowExceptionIfIdIsEmpty() throws InternalException, CloudException {
		new AzureAffinityGroupSupport(azureMock).get("");
	}
	
	@Test
	public void listShouldReturnCorrectResult() throws InternalException, CloudException {
		
		final String filteroutDataCenterId = REGION + "_INVALID";
		final String filteroutName = REGION + "0";
		
		AffinityGroupsModel model = new AffinityGroupsModel();
		model.setAffinityGroups(Arrays.asList(
				createAffinityGroupModel(AFFINITY_GROUP_ID, REGION, AFFINITY_GROUP_ID), 
				createAffinityGroupModel(filteroutName, REGION, filteroutName),
				createAffinityGroupModel(AFFINITY_GROUP_ID, filteroutDataCenterId, AFFINITY_GROUP_ID)));
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<AffinityGroupsModel>(model),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(RESOURCE_AFFINITYGROUPS, ENDPOINT, ACCOUNT_NO));
            	return responseMock;
            }
		};
		
		AffinityGroup expectedResult = AffinityGroup.getInstance(
				AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, REGION, null);
		
		AffinityGroupFilterOptions filter = AffinityGroupFilterOptions.getInstance("^[A-Za-z]+").withDataCenterId(REGION);
		Iterator<AffinityGroup> resultIter = new AzureAffinityGroupSupport(azureMock).list(filter).iterator();
		AffinityGroup actualResult = resultIter.next();
		assertReflectionEquals("match fields for affinity group failed", expectedResult, actualResult);
		assertFalse("only one valid result should be found", resultIter.hasNext());
	}
	
	@Test
	public void modifyShouldPutWithCorrectRequest() throws InternalException, CloudException {
		
		final String updatedDescription = AFFINITY_GROUP_ID + "_UPDATED";
		
		UpdateAffinityGroupModel updateAffinityGroupModel = new UpdateAffinityGroupModel();
        updateAffinityGroupModel.setDescription(updatedDescription);
		
		final CloseableHttpResponse modifyResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<UpdateAffinityGroupModel>(updateAffinityGroupModel),
				new Header[]{});
		
		final CloseableHttpResponse getResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<AffinityGroupModel>(createAffinityGroupModel(AFFINITY_GROUP_ID, REGION, updatedDescription)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
	        		assertPut(request, String.format(RESOURCE_AFFINITYGROUP, ENDPOINT, ACCOUNT_NO, AFFINITY_GROUP_ID));
	            	return modifyResponseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(RESOURCE_AFFINITYGROUP, ENDPOINT, ACCOUNT_NO, AFFINITY_GROUP_ID));
	            	return getResponseMock;
            	} else {
            		throw new RuntimeException("invalid invocation count");
            	}
            }
		};
		
		AffinityGroup expectedResult = AffinityGroup.getInstance(AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, updatedDescription, REGION, null);
		
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, updatedDescription, REGION);
		AffinityGroup actualResult = new AzureAffinityGroupSupport(azureMock).modify(AFFINITY_GROUP_ID, options);
		assertReflectionEquals("match fields for affinity group failed", expectedResult, actualResult);
	}
	
	@Test(expected=InternalException.class)
	public void modifyShouldThrowExceptionIfIdIsNull() throws InternalException, CloudException {
		final String updatedDescription = AFFINITY_GROUP_ID + "_UPDATED";
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, updatedDescription, REGION);
		new AzureAffinityGroupSupport(azureMock).modify(null, options);
	}
	
	@Test(expected=InternalException.class)
	public void modifyShouldThrowExceptionIfIdIsEmpty() throws InternalException, CloudException {
		final String updatedDescription = AFFINITY_GROUP_ID + "_UPDATED";
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, updatedDescription, REGION);
		new AzureAffinityGroupSupport(azureMock).modify("", options);
	}
	
	@Test(expected=InternalException.class)
	public void modifyShouldThrowExceptionIfOptionsIsNull() throws InternalException, CloudException {
		new AzureAffinityGroupSupport(azureMock).modify(AFFINITY_GROUP_ID, null);
	}
	
	@Test(expected=InternalException.class)
	public void modifyShouldThrowExceptionIfDescriptionIsNull() throws InternalException, CloudException {
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, null, REGION);
		new AzureAffinityGroupSupport(azureMock).modify(AFFINITY_GROUP_ID, options);
	}
	
	@Test(expected=InternalException.class)
	public void modifyShouldThrowExceptionIfParsingRequestEntityFailed() throws InternalException, CloudException {
		new MockUp<AzureMethod>() {
			@Mock
			<T> String put(String resource, T object) throws JAXBException, CloudException, InternalException {
				throw new JAXBException("parsing object failed");
			}
		};
		AffinityGroupCreateOptions options = AffinityGroupCreateOptions.getInstance(AFFINITY_GROUP_ID, AFFINITY_GROUP_ID, REGION);
		new AzureAffinityGroupSupport(azureMock).modify(AFFINITY_GROUP_ID, options);
	}
	
	private AffinityGroupModel createAffinityGroupModel(String id, String region, String description) {
		
		AffinityGroupModel model = new AffinityGroupModel();
		model.setCapabilities(Arrays.asList("PersistentVMRole"));
		model.setCreatedTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		model.setDescription(description);
		model.setLabel(id);
		model.setLocation(region);
		model.setName(id);
		
		ComputeCapabilities computeCapabilities = new ComputeCapabilities();
		computeCapabilities.setVirtualMachineRoleSizes(Arrays.asList("TESTVMROLESIZE"));
		computeCapabilities.setWebWorkerRoleSizes(Arrays.asList("TESTWEBWORKERROLESIZE"));
		model.setComputeCapabilities(computeCapabilities);
		
		HostedService storageService = new HostedService();
		storageService.setServiceName("TESTSTORAGESERVICE");
		storageService.setUrl("http://azure.microsoft.com/storageservices/" + storageService.getServiceName());
		model.setStorageServices(Arrays.asList(storageService));
		
		HostedService hostedService = new HostedService();
		hostedService.setServiceName("TESTHOSTEDSERVICE");
		hostedService.setUrl("http://azure.microsoft.com/hostedservices/" + hostedService.getServiceName());
		model.setHostedServices(Arrays.asList(hostedService));
		
		return model;
    }
}
