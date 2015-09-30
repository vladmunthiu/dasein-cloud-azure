package org.dasein.cloud.azure.storage.model;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name="Container")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainerModel {

	@XmlElement(name="Name")
	private String name;
	
	@XmlElement(name="Properties")
	private BasePropertiesModel properties;
	
	@XmlJavaTypeAdapter(MapAdapter.class)
	private Map<String, Object> metadata;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BasePropertiesModel getProperties() {
		return properties;
	}

	public void setProperties(BasePropertiesModel properties) {
		this.properties = properties;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}
