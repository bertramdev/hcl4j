package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ListPrimitiveType extends SubTypePrimitiveType {

	public ListPrimitiveType(PrimitiveType subType, Integer line, Integer column,Integer position) {
		super(subType,"list",line,column,position);
	}

	public String getSymbolName() {
		return "ListPrimitiveType";
	}
}
