package org.dasein.cloud.azure.storage.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name="Blob")
@XmlAccessorType(XmlAccessType.FIELD)
public class BlobModel {

	@XmlElement(name="Name")
	private String name;
	@XmlElement(name="Snapshot")
	private String snapshot;
	@XmlElement(name="Properties")
	private BlobPropertiesModel properties;
	@XmlJavaTypeAdapter(MapAdapter.class)
	private Map<String, String> metadata;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSnapshot() {
		return snapshot;
	}
	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}
	public BlobPropertiesModel getProperties() {
		return properties;
	}
	public void setProperties(BlobPropertiesModel properties) {
		this.properties = properties;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public void addMetadata(String key, String value) {
		if (this.metadata == null) {
			this.metadata = new HashMap<String, String>();
		}
		this.metadata.put(key, value);
	}
}
