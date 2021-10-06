package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class BooleanPrimitiveType extends PrimitiveType {
	public BooleanPrimitiveType(Integer line, Integer column,Integer position) {
		super("boolean",line,column,position);
	}

	public String getSymbolName() {
		return "BooleanPrimitiveType";
	}
}
