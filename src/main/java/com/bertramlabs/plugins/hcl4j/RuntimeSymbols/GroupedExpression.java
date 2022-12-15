package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

import com.bertramlabs.plugins.hcl4j.symbols.GenericSymbol;

public class GroupedExpression extends GenericSymbol {
    public GroupedExpression(Integer line, Integer column,Integer position) {
        super(null,line,column,position);
    }

    public String getSymbolName() {
        return "GroupedExpression";
    }
}
