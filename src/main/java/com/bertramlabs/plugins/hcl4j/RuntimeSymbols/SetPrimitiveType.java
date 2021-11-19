package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class SetPrimitiveType extends SubTypePrimitiveType {

	public SetPrimitiveType(PrimitiveType subType, Integer line, Integer column,Integer position) {
		super(subType,"set",line,column,position);
	}

	public String getSymbolName() {
		return "SetPrimitiveType";
	}
}
