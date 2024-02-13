package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ForSource extends EvalSymbol{
	public ForSource(Integer line, Integer column,Integer position) {
		super(null,line,column,position);
	}

	public String getSymbolName() {
		return "ForSource";
	}
}
