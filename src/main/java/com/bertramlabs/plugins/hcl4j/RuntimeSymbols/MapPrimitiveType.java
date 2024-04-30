package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class MapPrimitiveType extends SubTypePrimitiveType {

	public MapPrimitiveType(PrimitiveType subType, Integer line, Integer column, Long position) {
		super(subType,"map",line,column,position);
	}

	public String getSymbolName() {
		return "MapPrimitiveType";
	}
}
