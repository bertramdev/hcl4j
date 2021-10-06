package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ListPrimitiveType extends PrimitiveType {

	public PrimitiveType subType;
	
	public ListPrimitiveType(PrimitiveType subType, Integer line, Integer column,Integer position) {
		super("list",line,column,position);
		this.subType = subType;
	}

	public String getSymbolName() {
		return "ListPrimitiveType";
	}
}
