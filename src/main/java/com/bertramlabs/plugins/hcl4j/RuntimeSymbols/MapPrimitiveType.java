package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class MapPrimitiveType extends PrimitiveType {
	public MapPrimitiveType(Integer line, Integer column,Integer position) {
		super("map",line,column,position);
	}

	public String getSymbolName() {
		return "MapPrimitiveType";
	}
}
