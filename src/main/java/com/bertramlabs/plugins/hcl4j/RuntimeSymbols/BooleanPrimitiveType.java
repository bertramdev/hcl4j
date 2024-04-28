package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class BooleanPrimitiveType extends PrimitiveType {
	public BooleanPrimitiveType(Integer line, Integer column, Long position) {
		super("boolean",line,column,position);
	}

	public String getSymbolName() {
		return "BooleanPrimitiveType";
	}
}
