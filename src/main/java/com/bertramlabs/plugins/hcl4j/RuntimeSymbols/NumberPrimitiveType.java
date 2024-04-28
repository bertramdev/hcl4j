package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class NumberPrimitiveType extends PrimitiveType {
	public NumberPrimitiveType(Integer line, Integer column, Long position) {
		super("number",line,column,position);
	}

	public String getSymbolName() {
		return "NumberPrimitiveType";
	}
}
