package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

public class AnyPrimitiveType extends PrimitiveType {
    public AnyPrimitiveType(Integer line, Integer column,Integer position) {
        super("any",line,column,position);
    }

    public String getSymbolName() {
        return "AnyPrimitiveType";
    }
}
