package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ForConditional extends EvalSymbol{
    public ForConditional(Integer line, Integer column,Integer position) {
        super(null,line,column,position);
    }

    public String getSymbolName() {
        return "ForConditional";
    }
}
