package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

import java.util.ArrayList;

public class Function extends EvalSymbol{
	public Function(String name, Integer line, Integer column,Integer position) {
		super(name,line,column,position);
	}

	public String getSymbolName() {
		return "Function";
	}


}
