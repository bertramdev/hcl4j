package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class Variable extends EvalSymbol {
	public Variable(String name) {
		this.name = name;
	}
	public String name;
}
