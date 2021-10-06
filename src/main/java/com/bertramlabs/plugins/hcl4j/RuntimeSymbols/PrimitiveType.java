package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class PrimitiveType extends EvalSymbol {
	public PrimitiveType(String name, Integer line, Integer column,Integer position) {
		super(name,line,column,position);
	}

	public String getSymbolName() {
		return "PrimitiveType";
	}
}
