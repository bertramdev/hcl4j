package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

import com.bertramlabs.plugins.hcl4j.symbols.GenericSymbol;
import com.bertramlabs.plugins.hcl4j.symbols.Symbol;

import java.util.ArrayList;

public class VariableTree extends Variable {
    public VariableTree(Integer line, Integer column,Integer position) {
        super(null,line,column,position);
    }

    public String getSymbolName() {
        return "VariableTree";
    }

    public String getName() {
        ArrayList<String> elementNames = new ArrayList<>();
        GenericSymbol currentSymbol = this;
        for(Symbol child : currentSymbol.getChildren()) {
            elementNames.add(child.getName());
        }
        return String.join(".",elementNames);
    }
}
