package org.dasein.cloud.azure.tests.storage;

import static org.dasein.cloud.azure.tests.HttpMethodAsserts.*;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.azure.storage.BlobStore;
import org.dasein.cloud.azure.storage.model.BasePropertiesModel;
import org.dasein.cloud.azure.storage.model.BlobModel;
import org.dasein.cloud.azure.storage.model.BlobPrefixModel;
import org.dasein.cloud.azure.storage.model.BlobPropertiesModel;
import org.dasein.cloud.azure.storage.model.BlobsEnumerationResultsModel;
import org.dasein.cloud.azure.storage.model.BlockListModel;
import org.dasein.cloud.azure.storage.model.BlockModel;
import org.dasein.cloud.azure.storage.model.ContainerEnumerationResultsModel;
import org.dasein.cloud.azure.storage.model.ContainerModel;
import org.dasein.cloud.azure.tests.AzureTestsBaseWithLocation;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.cloud.util.requester.entities.DaseinObjectToXmlEntity;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Byte;
import org.dasein.util.uom.storage.Storage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.apache.commons.codec.binary.Base64;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

public class AzureBlobStoreTest extends AzureTestsBaseWithLocation {

	private final String REQUEST_OBJECT_URL = "%s/%s/%s";
	private final String REQUEST_BUCKET_URL = "%s/%s?restype=container";
	private final String UPLOAD_FILE_URL = REQUEST_OBJECT_URL + "?timeout=600";
	private final String UPLOAD_LARGE_FILE_URL = REQUEST_OBJECT_URL + "?comp=block&blockid=%s";
	private final String GET_BLOCKS_LIST_URL = REQUEST_OBJECT_URL + "?comp=blocklist&blocklisttype=all";
	private final String COMMIT_BLOCKS_URL = REQUEST_OBJECT_URL + "?comp=blocklist";
	private final String LIST_BUCKETS_URL = "%s/?comp=list";
	private final String LIST_OBJECTS_URL = "%s/%s?comp=list&restype=container";
	private final String MAKE_BUCKET_PUBLIC_URL = "%s/%s?comp=acl&restype=container";
	private final String MAKE_OBJECT_PUBLIC_URL = REQUEST_OBJECT_URL + "?comp=acl&restype=container";
	
	private final Integer FILE_SIZE = 1024;
	private final Integer LARGE_FILE_SIZE = 64 * 1024 * 1024;
	
	private final String BUCKET_ID = "TESTBUCKET";
	private final String OBJECT_ID = "TESTOBJECT";
	private final String STORAGE_SERVICE = "TESTSTORAGESERVICE";

	@Rule
    public final TestName name = new TestName();
	
	private DateFormat format;
	
	@Before
	public void initialize() throws CloudException, InternalException, IOException {
		
		new NonStrictExpectations() {
			{ azureMock.getStorageEndpoint(); result = ENDPOINT; }
			{ azureMock.getStorageService(); result = STORAGE_SERVICE; }
			{ providerContextMock.getStoragePrivate(); result = "STORAGEPRIVATEKEY".getBytes(); }
		};
		
		String methodName = name.getMethodName();
		if (methodName.startsWith("upload") || methodName.startsWith("getBucket") || 
				methodName.startsWith("getObject") || methodName.startsWith("renameBucket")) {
			new NonStrictExpectations() {
				{ azureMock.hold(); }
				{ azureMock.release(); }
			};
		}
		
		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Test
	public void allowsNestedBucketsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertFalse("azure not allow nested buckets", new BlobStore(azureMock).allowsNestedBuckets());
	}
	
	@Test
	public void allowsRootObjectsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertFalse("azure not allow root objects", new BlobStore(azureMock).allowsRootObjects());
	}
	
	@Test
	public void allowsPublicSharingShouldReturnCorrectResult() throws CloudException, InternalException {
		assertFalse("azure not allow public sharing", new BlobStore(azureMock).allowsPublicSharing());
	}
	
	@Test
	public void getMaxBucketsShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals("match max buckets failed", BlobStore.MAX_BUCKETS, new BlobStore(azureMock).getMaxBuckets());
	}
	
	@Test
	public void getMaxObjectSizeShouldReturnCorrectResult() {
		assertEquals("match max objects size failed", 
				BlobStore.MAX_OBJECT_SIZE, new BlobStore(azureMock).getMaxObjectSize());
	}
	
	@Test
	public void getMaxObjectsPerBucketShouldReturnCorrectResult() throws CloudException, InternalException {
		assertEquals("match max objects count of bucket failed", 
				BlobStore.MAX_OBJECTS, new BlobStore(azureMock).getMaxObjectsPerBucket());
	}
	
	@Test
	public void getProviderTermForBucketShouldReturnCorrectResult() {
		assertEquals("match term for bucket failed", "bucket", new BlobStore(azureMock).getProviderTermForBucket(null));
	}
	
	@Test
	public void getProviderTermForObjectShouldReturnCorrectResult() {
		assertEquals("match term for object failed", "object", new BlobStore(azureMock).getProviderTermForObject(null));
	}
	
	@Test
	public void getBucketNameRulesShouldReturnCorrectResult() throws CloudException, InternalException {
		NamingConstraints namingConstraints = new BlobStore(azureMock).getBucketNameRules();
		NamingConstraints expectedNamingConstraints = 
				NamingConstraints.getAlphaNumeric(1, 255).constrainedBy(new char[] { '-', '.' }).limitedToLatin1().lowerCaseOnly();
		assertReflectionEquals("match bucket name rules failed", expectedNamingConstraints, namingConstraints);
	}
	
	@Test
	public void getObjectNameRulesShouldReturnCorrectResult() throws CloudException, InternalException {
		NamingConstraints namingConstraints = new BlobStore(azureMock).getObjectNameRules();
		NamingConstraints expectedNamingConstraints = 
				NamingConstraints.getAlphaNumeric(1, 255).constrainedBy(new char[] { '-', '.', ',', '#', '+' }).limitedToLatin1().lowerCaseOnly();
		assertReflectionEquals("match object name rules failed", expectedNamingConstraints, namingConstraints);
	}
	
	@Test
	public void existsShouldReturnCorrectResultIfBucketFound() throws InternalException, CloudException {
		
		final ContainerModel targetModel = new ContainerModel();
		targetModel.setName(BUCKET_ID);
		targetModel.setProperties(createBlobBucketProperties());
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            	return responseMock;
            }
		};
		
		assertTrue("find target bucket failed", new BlobStore(azureMock).exists(BUCKET_ID));
	}
	
	@Test
	public void existsShouldReturnCorrectResultIfNoBucketFound() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            	return responseMock;
            }
		};
		
		assertFalse("find not exist bucket", new BlobStore(azureMock).exists(BUCKET_ID));
	}
	
	@Test
	public void uploadShouldReturnCorrectResult(@Mocked final FileInputStream fis) throws CloudException, InternalException, IOException, AssertionFailedError, ParseException {
		
		File tempFile = new MockUp<File>(){
			@Mock
			long length() {
				return FILE_SIZE;
			}
		}.getMockInstance();
		
		final CloseableHttpResponse uploadObjectResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		BlobPropertiesModel properties = createBlobObjectProperties();
		
		BlobModel targetModel = new BlobModel();
		targetModel.setName(OBJECT_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse listObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
	            	assertPut(request, String.format(UPLOAD_FILE_URL, ENDPOINT, BUCKET_ID, OBJECT_ID), 
	            			new Header[]{new BasicHeader("x-ms-blob-type", "BlockBlob"), new BasicHeader("content-type", "application/octet-stream")});
	            	return uploadObjectResponseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
            		return listObjectsResponseMock;
            	} else {
            		throw new RuntimeException("invalid invocation count!");
            	}
            }
        };
		
		Blob result = new AzureBlobStoreSupport(azureMock, true).upload(tempFile, BUCKET_ID, OBJECT_ID);
		assertReflectionEquals("match fields for BlobsEnumerationResultsModel failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, OBJECT_ID, format.parse(properties.getLastModified()).getTime(), new Storage<Byte>(FILE_SIZE, Storage.BYTE)), 
				result);
	}
	
	@Test
	public void uploadLargeFileShouldReturnCorrectResult(@Mocked final FileInputStream fis) throws IOException, CloudException, InternalException, AssertionFailedError, ParseException {
		
		File tempFile = new MockUp<File>(){}.getMockInstance(); 
		
		new NonStrictExpectations() {
			{ fis.available(); result = LARGE_FILE_SIZE; }
			{ fis.read((byte[]) any); result = LARGE_FILE_SIZE / 16; }
		};
		
		final CloseableHttpResponse putResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		final BlockListModel blockListModel = new BlockListModel();
		final List<BlockModel> uncommittedBlocksModel = new ArrayList<BlockModel>();
		for (int i = 0; i < 16; i++) {
			String blockId = Base64.encodeBase64String(String.valueOf(1000 + i).getBytes());
        	
    		BlockModel block = new BlockModel();
        	block.setName(blockId);
        	block.setSize(LARGE_FILE_SIZE / 16);
        	uncommittedBlocksModel.add(block);
		}
		blockListModel.setUncommittedBlocks(uncommittedBlocksModel);
		
		final CloseableHttpResponse getBlocksResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlockListModel>(blockListModel),
				new Header[]{});
		
		BlobModel targetModel = new BlobModel();
		targetModel.setName(OBJECT_ID);
		BlobPropertiesModel properties = createBlobObjectProperties();
		properties.setContentLength(LARGE_FILE_SIZE);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse listObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 19)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() <= 16) {
	            	assertPut(request, String.format(UPLOAD_LARGE_FILE_URL, ENDPOINT, BUCKET_ID, OBJECT_ID, 
	            			Base64.encodeBase64String(String.valueOf(999 + inv.getInvocationCount()).getBytes())), 
	            			new Header[]{new BasicHeader("x-ms-blob-type", "BlockBlob"), new BasicHeader("content-type", "text/plain")});
	            	try {
		            	if (inv.getInvocationCount() == 15) {
		            		new NonStrictExpectations() {
								{ fis.read((byte[]) any); result = LARGE_FILE_SIZE / 16 - 1; }
							};
		            	} else if (inv.getInvocationCount() == 16) {
							new NonStrictExpectations() {
								{ fis.read((byte[]) any); result = -1; }
							};
		            	}
	            	} catch (IOException e) {
						//No-op
					}
	            	return putResponseMock;
            	} else if (inv.getInvocationCount() == 17) {
            		assertGet(request, String.format(GET_BLOCKS_LIST_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
            		return getBlocksResponseMock;
            	} else if (inv.getInvocationCount() == 18) {
            		assertPut(request, String.format(COMMIT_BLOCKS_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
            		return putResponseMock;
            	} else if (inv.getInvocationCount() == 19) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
            		return listObjectsResponseMock;
            	} else {
            		throw new RuntimeException("invalid invocation count!");
            	}
            }
        };
		
		Blob result = new AzureBlobStoreSupport(azureMock, true).upload(tempFile, BUCKET_ID, OBJECT_ID);
		assertReflectionEquals("match fields for BlobsEnumerationResultsModel failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, OBJECT_ID, format.parse(properties.getLastModified()).getTime(), 
						new Storage<Byte>(LARGE_FILE_SIZE, Storage.BYTE)), 
				result);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void uploadShouldThrowExceptionIfUploadRootObject() throws CloudException, InternalException {
		new BlobStore(azureMock).upload(
				new MockUp<File>(){}.getMockInstance(), 
				null, 
				OBJECT_ID);
	}
	
	@Test
	public void createBucketShouldPutWithCorrectRequest() throws InternalException, CloudException, AssertionFailedError, ParseException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		BasePropertiesModel properties = createBlobBucketProperties();
		
		ContainerModel targetModel = new ContainerModel();
		targetModel.setName(BUCKET_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse listBucketsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 2)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
	            	assertPut(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, BUCKET_ID));
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            		return listBucketsResponseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
		
		Blob resultBucket = new AzureBlobStoreSupport(azureMock, false).createBucket(BUCKET_ID, false);
		assertReflectionEquals("match fields for new bucket failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, format.parse(properties.getLastModified()).getTime()), 
				resultBucket);
	}
	
	@Test
	public void createBucketWithFindFreeNameShouldPutWithCorrectRequest() throws CloudException, InternalException, AssertionFailedError, ParseException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		BasePropertiesModel properties = createBlobBucketProperties();
		
		final ContainerModel targetModel = new ContainerModel();
		targetModel.setName(BUCKET_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse existBucketsInv1ResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		targetModel.setName(BUCKET_ID + "-1");
		
		final CloseableHttpResponse existBucketsInv2ResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel()),
				new Header[]{});
		
		final CloseableHttpResponse listBucketsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 4)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
	            	return existBucketsInv1ResponseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
	            	return existBucketsInv2ResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
	            	assertPut(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, targetModel.getName()));
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 4) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            		return listBucketsResponseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
		
		Blob resultBucket = new BlobStore(azureMock).createBucket(BUCKET_ID, true);
		assertReflectionEquals("match fields for new bucket failed", 
				Blob.getInstance(REGION, null, targetModel.getName(), format.parse(properties.getLastModified()).getTime()), 
				resultBucket);
	}
	
	@Test(expected = OperationNotSupportedException.class)
	public void createBucketShouldThrowExceptionIfBucketNameIsInvalid() throws InternalException, CloudException {
		new BlobStore(azureMock).createBucket("TESTPREFIX/" + BUCKET_ID, false);
	}
	
	@Test(expected = CloudException.class)
	public void createBucketShouldThrowExceptionIfBucketNotFound() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		BasePropertiesModel properties = createBlobBucketProperties();
		
		final ContainerModel targetModel = new ContainerModel();
		targetModel.setName(BUCKET_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse existBucketsInv1ResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		targetModel.setName(BUCKET_ID + "-1");
		
		final CloseableHttpResponse existBucketsInv2ResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel()),
				new Header[]{});
		
		final CloseableHttpResponse listBucketsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 4)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
	            	return existBucketsInv1ResponseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
	            	return existBucketsInv2ResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
	            	assertPut(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, targetModel.getName()));
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 4) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            		return listBucketsResponseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
		new BlobStore(azureMock).createBucket(BUCKET_ID, true);
	}
	
	@Test
	public void getBucketShouldReturnCorrectResult() throws InternalException, CloudException, AssertionFailedError, ParseException {
		
		BasePropertiesModel properties = createBlobBucketProperties();
		
		final ContainerModel targetModel = new ContainerModel();
		targetModel.setName(BUCKET_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            	return responseMock;
            }
		};
		
		Blob resultBucket = new BlobStore(azureMock).getBucket(BUCKET_ID);
		assertReflectionEquals("match fields for new bucket failed", 
				Blob.getInstance(REGION, null, targetModel.getName(), format.parse(properties.getLastModified()).getTime()), 
				resultBucket);
	}
	
	@Test
	public void getBucketShouldReturnNullIfNoBucketFound() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            	return responseMock;
            }
		};
		
		assertNull("find not exist bucket", new BlobStore(azureMock).getBucket(BUCKET_ID));
	}
	
	@Test
	public void removeBucketShouldDeleteWithCorrectRequest() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertDelete(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, BUCKET_ID));
        		return responseMock;
            }
        };
		
		new BlobStore(azureMock).removeBucket(BUCKET_ID);
	}
	
	@Test
	public void renameBucketShouldPutWithCorrectRequest() throws CloudException, InternalException {
		
		final String sourceBucketName = BUCKET_ID + "_SOURCE";
		final String targetBucketName = BUCKET_ID + "_TARGET";
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		ContainerModel targetModel = new ContainerModel();
		targetModel.setName(targetBucketName);
		targetModel.setProperties(createBlobBucketProperties());
		
		final CloseableHttpResponse listBucketsWithTargetBucketResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(targetModel)),
				new Header[]{});
		
		BlobModel object = new BlobModel();
		object.setName(OBJECT_ID);
		object.setProperties(createBlobObjectProperties());
		
		final CloseableHttpResponse listObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(object)),
				new Header[]{});
		
		final CloseableHttpResponse listEmptyObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 8)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
	            	assertPut(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, targetBucketName));
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            		return listBucketsWithTargetBucketResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, sourceBucketName));
                	return listObjectsResponseMock;
            	} else if (inv.getInvocationCount() == 4) {
            		assertPut(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, targetBucketName, OBJECT_ID));
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 5) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, targetBucketName));
                	return listObjectsResponseMock;
            	} else if (inv.getInvocationCount() == 6) {
            		assertDelete(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, sourceBucketName, OBJECT_ID));
            		return responseMock;
            	} else if (inv.getInvocationCount() == 7) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, sourceBucketName));
                	return listEmptyObjectsResponseMock;
            	} else if (inv.getInvocationCount() == 8) {
            		assertDelete(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, sourceBucketName));
            		return responseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
        
        String resultBucketName = new BlobStore(azureMock).renameBucket(sourceBucketName, targetBucketName, false);
        assertEquals("match new bucket name failed", targetBucketName, resultBucketName);
	}
	
	@Test
	public void renameObjectShouldPutWithCorrectRequest() throws CloudException, InternalException {
		
		final String sourceObjectName = OBJECT_ID + "_SOURCE";
		final String targetObjectName = OBJECT_ID + "_TARGET";
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		BlobModel object = new BlobModel();
		object.setName(targetObjectName);
		object.setProperties(createBlobObjectProperties());
		
		final CloseableHttpResponse listObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(object)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 3)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
            		assertPut(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, targetObjectName), 
            				new Header[]{new BasicHeader("x-ms-copy-source", String.format("/%s/%s/%s", STORAGE_SERVICE, BUCKET_ID, sourceObjectName))});
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
                	return listObjectsResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
            		assertDelete(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, sourceObjectName));
            		return responseMock;
            	} else {
            		throw new RuntimeException("Invalid invocation count!");
            	}
            }
        };
		
		new BlobStore(azureMock).renameObject(BUCKET_ID, sourceObjectName, targetObjectName);
	}
	
	@Test(expected=CloudException.class)
	public void renameObjectShouldThrowExceptionIfBucketNameIsNull() throws CloudException, InternalException {
		
		final String sourceObjectName = OBJECT_ID + "_SOURCE";
		final String targetObjectName = OBJECT_ID + "_TARGET";
		
		new BlobStore(azureMock).renameObject(null, sourceObjectName, targetObjectName);
	}
	
	@Test
	public void getObjectShouldReturnCorrectResult() throws InternalException, CloudException, AssertionFailedError, ParseException {
		
		BlobPropertiesModel properties = createBlobObjectProperties();
		
		BlobModel targetModel = new BlobModel();
		targetModel.setName(OBJECT_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
        		return responseMock;
            }
        };
        
        Blob resultObject = new BlobStore(azureMock).getObject(BUCKET_ID, OBJECT_ID);
        assertReflectionEquals("match fields for BlobsEnumerationResultsModel failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, OBJECT_ID, format.parse(properties.getLastModified()).getTime(), new Storage<Byte>(properties.getContentLength(), Storage.BYTE)), 
				resultObject);
	}
	
	@Test
	public void getObjectShouldReturnNullIfNoObjectFound() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
        		return responseMock;
            }
        };
        
        assertNull("find not exist object", new BlobStore(azureMock).getObject(BUCKET_ID, OBJECT_ID));
	}
	
	@Test
	public void getObjectShouldReturnCorrectResultIfBucketNameIsNull() throws InternalException, CloudException {
		assertNull("find not exist object", new BlobStore(azureMock).getObject(null, OBJECT_ID));
	}
	
	@Test
	public void getObjectSizeShouldReturnCorrectResult() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{new BasicHeader("Content-Length", "11")});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
        		return responseMock;
            }
        };
		
		Storage<Byte> resultSize = new BlobStore(azureMock).getObjectSize(BUCKET_ID, OBJECT_ID);
		assertEquals("match object size failed", 11, resultSize.getQuantity().intValue());
	}
	
	@Test
	public void getObjectSizeShouldReturnNullIfNoSizeFound() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
        		return responseMock;
            }
        };
		
		assertNull("find not exist size", new BlobStore(azureMock).getObjectSize(BUCKET_ID, OBJECT_ID));
	}
	
	@Test
	public void removeObjectShouldDeleteWithCorrectRequest() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertDelete(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
        		return responseMock;
            }
        };
		
		new BlobStore(azureMock).removeObject(BUCKET_ID, OBJECT_ID);
	}
	
	@Test
	public void isObjectPublicShouldReturnCorrectResultForPublicObject() throws CloudException, InternalException {
		
		BlobPropertiesModel properties = createBlobObjectProperties();
		
		BlobModel targetModel = new BlobModel();
		targetModel.setName(OBJECT_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
        		return responseMock;
            }
        };
		
		assertTrue("find public object failed", new BlobStore(azureMock).isPublic(BUCKET_ID, OBJECT_ID));
	}
	
	@Test
	public void isObjectPublicShouldReturnCorrectResultForPrivateObject() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
        		return responseMock;
            }
        };
		
		assertFalse("find public object failed", new BlobStore(azureMock).isPublic(BUCKET_ID, OBJECT_ID));
	}
	
	@Test
	public void isBucketPublicShouldReturnCorrectResult() throws CloudException, InternalException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel()),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(REQUEST_BUCKET_URL, ENDPOINT, BUCKET_ID));
        		return responseMock;
            }
        };
		
        assertTrue("find public bucket failed", new BlobStore(azureMock).isPublic(BUCKET_ID, null));
	}
	
	@Test
	public void isPublicShouldReturnCorrectResultIfCheckRootObject() throws CloudException, InternalException {
		assertFalse("not support root object public", new BlobStore(azureMock).isPublic(null, OBJECT_ID));
	}
	
	@Test
	public void isPublicShouldReturnCorrectResultIfBucketAndObjectNameIsNull() throws CloudException, InternalException {
		assertFalse("find public bucket with invalid params", new BlobStore(azureMock).isPublic(null, null));
	}
	
	@Test
	public void listBucketsShouldReturnCorrectResult() throws CloudException, InternalException, AssertionFailedError, ParseException {
		
		BasePropertiesModel properties = createBlobBucketProperties();
		
		ContainerModel model1 = new ContainerModel();
		model1.setName(BUCKET_ID + "1");
		model1.setProperties(properties);
		
		ContainerModel model2 = new ContainerModel();
		model2.setName(BUCKET_ID + "2");
		model2.setProperties(properties);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<ContainerEnumerationResultsModel>(createBlobBucketsModel(model1, model2)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_BUCKETS_URL, ENDPOINT));
            	return responseMock;
            }
		};
		
		Iterator<Blob> buckets = new BlobStore(azureMock).list(null).iterator();
		assertReflectionEquals("match result bucket " + model1.getName() + " failed", 
				Blob.getInstance(REGION, null, model1.getName(), format.parse(properties.getLastModified()).getTime()),
				buckets.next());
		assertReflectionEquals("match result bucket " + model2.getName() + " failed", 
				Blob.getInstance(REGION, null, model2.getName(), format.parse(properties.getLastModified()).getTime()),
				buckets.next());
		assertFalse("no more bucket should be found", buckets.hasNext());
	}
	
	@Test
	public void listObjectsShouldReturnCorrectResult() throws CloudException, InternalException, AssertionFailedError, ParseException {
		
		BlobPropertiesModel properties = createBlobObjectProperties();
		
		BlobModel model1 = new BlobModel();
		model1.setName(OBJECT_ID + "1");
		model1.setProperties(properties);
		
		BlobModel model2 = new BlobModel();
		model2.setName(OBJECT_ID + "2");
		model2.setProperties(properties);
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(model1, model2)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, BUCKET_ID));
            	return responseMock;
            }
		};
		
		Iterator<Blob> buckets = new BlobStore(azureMock).list(BUCKET_ID).iterator();
		assertReflectionEquals("match result bucket " + model1.getName() + " failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, model1.getName(), format.parse(properties.getLastModified()).getTime(), new Storage<Byte>(properties.getContentLength(), Storage.BYTE)),
				buckets.next());
		assertReflectionEquals("match result bucket " + model2.getName() + " failed", 
				Blob.getInstance(REGION, null, BUCKET_ID, model2.getName(), format.parse(properties.getLastModified()).getTime(), new Storage<Byte>(properties.getContentLength(), Storage.BYTE)),
				buckets.next());
		assertFalse("no more object should be found", buckets.hasNext());
	}
	
	@Test
	public void makeBucketPublicShouldPutWithCorrectRequest() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertPut(request, String.format(MAKE_BUCKET_PUBLIC_URL, ENDPOINT, BUCKET_ID), 
        				new Header[]{new BasicHeader("x-ms-blob-public-access", "container")});
            	return responseMock;
            }
		};
		
		new BlobStore(azureMock).makePublic(BUCKET_ID);
	}
	
	@Test
	public void makeObjectPublicShouldPutWithCorrectRequest() throws InternalException, CloudException {
		
		final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 1)
            public CloseableHttpResponse execute(HttpUriRequest request) {
        		assertPut(request, String.format(MAKE_OBJECT_PUBLIC_URL, ENDPOINT, BUCKET_ID, OBJECT_ID), 
        				new Header[]{new BasicHeader("x-ms-blob-public-access", "container")});
            	return responseMock;
            }
		};
		
		new BlobStore(azureMock).makePublic(BUCKET_ID, OBJECT_ID);
	}
	
	@Test(expected = CloudException.class)
	public void makePublicShouldThrowExceptionIfBucketAndObjectNameIsNull() throws InternalException, CloudException {
		new BlobStore(azureMock).makePublic(null, null);
	}
	
	@Test
	public void getSignedObjectUrlShouldReturnCorrectResult() throws InternalException, CloudException {
		assertNull("signed object url should be null", new BlobStore(azureMock).getSignedObjectUrl(BUCKET_ID, OBJECT_ID, "1000"));
	}
	
	@Test
	public void moveShouldPutWithCorrectRequest() throws InternalException, CloudException {
		
		final String targetBucketName = BUCKET_ID + "_TARGET";
	
		new MockUp<System>() {
    		@Mock
    		long currentTimeMillis(Invocation inv) {
    			return CalendarWrapper.MINUTE * 25L * inv.getInvocationCount();
    		}
    	};
    	new MockUp<Thread>() {
    		@Mock
    		void sleep(long millis) throws InterruptedException {
    			//No-Op
    		}
    	};
		
    	final CloseableHttpResponse responseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				null,
				new Header[]{});
    	
    	BlobPropertiesModel properties = createBlobObjectProperties();
		
		BlobModel targetModel = new BlobModel();
		targetModel.setName(OBJECT_ID);
		targetModel.setProperties(properties);
		
		final CloseableHttpResponse listObjectsResponseMock = getHttpResponseMock(
				getStatusLineMock(HttpServletResponse.SC_OK),
				new DaseinObjectToXmlEntity<BlobsEnumerationResultsModel>(createBlobObjectsModel(targetModel)),
				new Header[]{});
		
		new MockUp<CloseableHttpClient>() {
            @Mock(invocations = 3)
            public CloseableHttpResponse execute(Invocation inv, HttpUriRequest request) {
            	if (inv.getInvocationCount() == 1) {
	        		assertPut(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, targetBucketName, OBJECT_ID), 
	        				new Header[]{new BasicHeader("x-ms-copy-source", String.format("/%s/%s/%s", STORAGE_SERVICE, BUCKET_ID, OBJECT_ID))});
	            	return responseMock;
            	} else if (inv.getInvocationCount() == 2) {
            		assertGet(request, String.format(LIST_OBJECTS_URL, ENDPOINT, targetBucketName));
            		return listObjectsResponseMock;
            	} else if (inv.getInvocationCount() == 3) {
            		assertDelete(request, String.format(REQUEST_OBJECT_URL, ENDPOINT, BUCKET_ID, OBJECT_ID));
            		return responseMock;
            	} else {
            		throw new RuntimeException("invalid invocation count");
            	}
            }
		};
    	
		new AzureBlobStoreSupport(azureMock, true).move(BUCKET_ID, OBJECT_ID, targetBucketName);
	}
	
	@Test(expected = CloudException.class)
	public void moveShouldThrowExceptionIfSourceBucketIsNull() throws InternalException, CloudException {
		new BlobStore(azureMock).move(null, OBJECT_ID, BUCKET_ID + "_TARGET");;
	}
	
	@Test(expected = CloudException.class)
	public void moveShouldThrowExceptionIfObjectIsNull() throws InternalException, CloudException {
		new BlobStore(azureMock).move(BUCKET_ID, null, BUCKET_ID + "_TARGET");;
	}
	
	@Test(expected = CloudException.class)
	public void moveShouldThrowExceptionIfTargetBucketIsNull() throws InternalException, CloudException {
		new BlobStore(azureMock).move(BUCKET_ID, OBJECT_ID, null);;
	}
	
	private ContainerEnumerationResultsModel createBlobBucketsModel(ContainerModel ... targetModel) {
		
		List<ContainerModel> containerModels = new ArrayList<ContainerModel>();
		
		if (targetModel != null && targetModel.length > 0) {
			for (ContainerModel model : targetModel) {
				containerModels.add(model);
			}
		}
		
		ContainerEnumerationResultsModel model = new ContainerEnumerationResultsModel();
		model.setContainers(containerModels);
		model.setMarker("TESTMARKER");
		if (targetModel != null) {
			model.setMaxResults(targetModel.length);
		} else {
			model.setMaxResults(0);
		}
		model.setPrefix("TESTPREFIX");
		model.setServiceEndpoint(String.format("http://%s.blob.core.windows.net/", ACCOUNT_NO));
		
		return model;
	}
	
	private BlobsEnumerationResultsModel createBlobObjectsModel(BlobModel ... targetModel) {
		
		BlobPrefixModel blobPrefixModel = new BlobPrefixModel();
		blobPrefixModel.setName("TESTPREFIX");
		
		List<BlobModel> blobs = new ArrayList<BlobModel>();
		
		if (targetModel != null && targetModel.length > 0) {
			for (BlobModel model : targetModel) {
				blobs.add(model);
			}
		}
		
		BlobsEnumerationResultsModel model = new BlobsEnumerationResultsModel();
		model.setBlobs(blobs);
		model.setContainerName(BUCKET_ID);
		model.setServiceEndpoint(String.format("http://%s.blob.core.windows.net/", ACCOUNT_NO));
		if (targetModel != null) {
			model.setMaxResults(targetModel.length);
		} else {
			model.setMaxResults(0);
		}
		model.setBlobPrefix(blobPrefixModel);
		
		return model;
	}
	
	private BasePropertiesModel createBlobBucketProperties() {
		
		BasePropertiesModel basePropertiesModel = new BasePropertiesModel();
		basePropertiesModel.seteTag("TESTETAG");
		basePropertiesModel.setLastModified(format.format(new Date()));
		basePropertiesModel.setLeaseDuration("infinite");
		basePropertiesModel.setLeaseState("available");
		basePropertiesModel.setLeaseStatus("unlocked");
		
		return basePropertiesModel;
	}
	
	private BlobPropertiesModel createBlobObjectProperties() {
		
		BlobPropertiesModel blobPropertiesModel = new BlobPropertiesModel();
		blobPropertiesModel.setBlobType("BlockBlob");
		blobPropertiesModel.setContentLength(FILE_SIZE);
		blobPropertiesModel.setContentType("PLAIN-TEXT");
		blobPropertiesModel.setCacheControl("NoCache");
		blobPropertiesModel.setContentEncoding("UTF-8");
		blobPropertiesModel.setContentLanguage("EN");
		blobPropertiesModel.setContentMd5("UREFIDJI9828DKDKD92I");
		blobPropertiesModel.seteTag("TESTETAG");
		blobPropertiesModel.setLastModified(format.format(new Date()));
		blobPropertiesModel.setLeaseStatus("unlocked");
		blobPropertiesModel.setLeaseState("available");
		
		return blobPropertiesModel;
	}
	
}
