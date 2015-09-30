package org.dasein.cloud.azure.storage.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="EnumerationResults")
@XmlAccessorType(XmlAccessType.FIELD)
public class BlobsEnumerationResultsModel extends BaseEnumerationResultsModel {
	
	@XmlAttribute(name="ServiceEndpoint")
	private String serviceEndpoint;
	@XmlAttribute(name="ContainerName")
	private String containerName;
	
	@XmlElement(name="Delimiter")
	private String delimiter;
	@XmlElementWrapper(name="Blobs")
	@XmlElement(name="Blob")
	private List<BlobModel> blobs;
	@XmlElement(name="BlobPrefix")
	private BlobPrefixModel blobPrefix;
	
	public String getServiceEndpoint() {
		return serviceEndpoint;
	}
	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}
	public String getContainerName() {
		return containerName;
	}
	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}
	public String getDelimiter() {
		return delimiter;
	}
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	public List<BlobModel> getBlobs() {
		return blobs;
	}
	public void setBlobs(List<BlobModel> blobs) {
		this.blobs = blobs;
	}
	public BlobPrefixModel getBlobPrefix() {
		return blobPrefix;
	}
	public void setBlobPrefix(BlobPrefixModel blobPrefix) {
		this.blobPrefix = blobPrefix;
	}
}
