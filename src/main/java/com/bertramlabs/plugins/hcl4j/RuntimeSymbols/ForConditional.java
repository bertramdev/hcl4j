package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class ForConditional extends GroupedExpression{
    public ForConditional(Integer line, Integer column, Long position) {
        super(line,column,position);
    }

    public String getSymbolName() {
        return "ForConditional";
    }
}
