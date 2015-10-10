package org.dasein.cloud.azure.storage.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Block")
@XmlAccessorType(XmlAccessType.FIELD)
public class BlockModel {

	@XmlElement(name="Name")
	private String name;
	@XmlElement(name="Size")
	private Integer size;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
	
}
