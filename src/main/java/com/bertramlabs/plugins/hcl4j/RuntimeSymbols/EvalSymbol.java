package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

import com.bertramlabs.plugins.hcl4j.symbols.GenericSymbol;

public class EvalSymbol extends GenericSymbol {
	@Override
	public String getSymbolName() {
		return "EvaluationSymbol";
	}

	public EvalSymbol(String name, Integer line, Integer column,Integer position) {
		super(name,line,column,position);
	}
}


