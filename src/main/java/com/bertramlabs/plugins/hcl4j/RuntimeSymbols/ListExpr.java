package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ListExpr extends EvalSymbol{
	public ListExpr(String name, Integer line, Integer column, Long position) {
		super(name,line,column,position);
	}

	public String getSymbolName() {
		return "ListExpr";
	}
}
