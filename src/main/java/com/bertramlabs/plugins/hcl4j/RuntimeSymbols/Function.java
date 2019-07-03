package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class Function extends EvalSymbol{
	String name;
	EvalSymbol[] args;
	EvalSymbol functionAttribute;
}
