package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class Operator extends EvalSymbol {
	public Operator(String name, Integer line, Integer column, Long position) {
		super(name,line,column,position);
	}

	public String getSymbolName() {
		return "Operator";
	}
}
