package org.framework.reconfigurationAlgorithm.enums;


public enum ResourcesEnum {
	CPU(0),
	RAM(1),
	NET(2);

	private Integer index;

	ResourcesEnum(Integer index){
		this.index = index;
	}

	public Integer getIndex(){
		return index;
	}

}
