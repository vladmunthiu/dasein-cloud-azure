package org.dasein.cloud.azure.storage.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="BlockList")
@XmlAccessorType(XmlAccessType.FIELD)
public class BlockListModel {

	@XmlElementWrapper(name="CommittedBlocks")
	@XmlElement(name="Block")
	private List<BlockModel> committedBlocks;
	
	@XmlElementWrapper(name="UncommittedBlocks")
	@XmlElement(name="Block")
	private List<BlockModel> uncommittedBlocks;

	public List<BlockModel> getCommittedBlocks() {
		return committedBlocks;
	}

	public void setCommittedBlocks(List<BlockModel> committedBlocks) {
		this.committedBlocks = committedBlocks;
	}

	public List<BlockModel> getUncommittedBlocks() {
		return uncommittedBlocks;
	}

	public void setUncommittedBlocks(List<BlockModel> uncommittedBlocks) {
		this.uncommittedBlocks = uncommittedBlocks;
	}
	
}
