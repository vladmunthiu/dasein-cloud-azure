package org.dasein.cloud.azure.storage.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class BlobPropertiesModel extends BasePropertiesModel {

	@XmlElement(name="Content-Length")
	private Integer contentLength;
	@XmlElement(name="Content-Type")
	private String contentType;
	@XmlElement(name="Content-Encoding")
	private String contentEncoding;
	@XmlElement(name="Content-Language")
	private String contentLanguage;
	@XmlElement(name="Content-MD5")
	private String contentMd5;
	@XmlElement(name="Cache-Control")
	private String cacheControl;
	@XmlElement(name="x-ms-blob-sequence-number")
	private String xMsBloblSequenceNumber;
	@XmlElement(name="BlobType")
	private String blobType;
	@XmlElement(name="CopyId")
	private String copyId;
	@XmlElement(name="CopyStatus")
	private String copyStatus;
	@XmlElement(name="copySource")
	private String copySource;
	@XmlElement(name="CopyProgress")
	private String CopyProgress;
	@XmlElement(name="CopyCompletionTime")
	private String copyCompletionTime;
	@XmlElement(name="CopyStatusDescription")
	private String copyStatusDescription;
	
	public Integer getContentLength() {
		return contentLength;
	}
	public void setContentLength(Integer contentLength) {
		this.contentLength = contentLength;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getContentEncoding() {
		return contentEncoding;
	}
	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}
	public String getContentLanguage() {
		return contentLanguage;
	}
	public void setContentLanguage(String contentLanguage) {
		this.contentLanguage = contentLanguage;
	}
	public String getContentMd5() {
		return contentMd5;
	}
	public void setContentMd5(String contentMd5) {
		this.contentMd5 = contentMd5;
	}
	public String getCacheControl() {
		return cacheControl;
	}
	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}
	public String getxMsBloblSequenceNumber() {
		return xMsBloblSequenceNumber;
	}
	public void setxMsBloblSequenceNumber(String xMsBloblSequenceNumber) {
		this.xMsBloblSequenceNumber = xMsBloblSequenceNumber;
	}
	public String getBlobType() {
		return blobType;
	}
	public void setBlobType(String blobType) {
		this.blobType = blobType;
	}
	public String getCopyId() {
		return copyId;
	}
	public void setCopyId(String copyId) {
		this.copyId = copyId;
	}
	public String getCopyStatus() {
		return copyStatus;
	}
	public void setCopyStatus(String copyStatus) {
		this.copyStatus = copyStatus;
	}
	public String getCopySource() {
		return copySource;
	}
	public void setCopySource(String copySource) {
		this.copySource = copySource;
	}
	public String getCopyProgress() {
		return CopyProgress;
	}
	public void setCopyProgress(String copyProgress) {
		CopyProgress = copyProgress;
	}
	public String getCopyCompletionTime() {
		return copyCompletionTime;
	}
	public void setCopyCompletionTime(String copyCompletionTime) {
		this.copyCompletionTime = copyCompletionTime;
	}
	public String getCopyStatusDescription() {
		return copyStatusDescription;
	}
	public void setCopyStatusDescription(String copyStatusDescription) {
		this.copyStatusDescription = copyStatusDescription;
	}
	
}
