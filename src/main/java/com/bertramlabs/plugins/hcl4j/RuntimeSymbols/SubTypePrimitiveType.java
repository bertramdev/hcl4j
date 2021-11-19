package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class SubTypePrimitiveType extends PrimitiveType {

	public PrimitiveType subType;
	
	public SubTypePrimitiveType(PrimitiveType subType,String name, Integer line, Integer column,Integer position) {
		super(name,line,column,position);
		this.subType = subType;
	}

	public String getSymbolName() {
		return "SubTypePrimitiveType";
	}
}
