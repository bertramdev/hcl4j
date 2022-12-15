package com.bertramlabs.plugins.hcl4j.RuntimeSymbols;

import java.util.ArrayList;

public class ComputedObject extends EvalSymbol {
    public ComputedObject(String name, Integer line, Integer column, Integer position) {
        super(name,line,column,position);
        this.variables = new ArrayList<>();
    }

    private EvalSymbol sourceExpression;
    private ArrayList<Variable> variables;
    private EvalSymbol targetExpression;
    private EvalSymbol conditionalExpression;


    public String getSymbolName() {
        return "ComputedObject";
    }

    public EvalSymbol getSourceExpression() {
        return sourceExpression;
    }

    public void setSourceExpression(EvalSymbol sourceExpression) {
        this.sourceExpression = sourceExpression;
    }

    public ArrayList<Variable> getVariables() {
        return variables;
    }

    public void setVariables(ArrayList<Variable> variables) {
        this.variables = variables;
    }

    public EvalSymbol getTargetExpression() {
        return targetExpression;
    }

    public void setTargetExpression(EvalSymbol targetExpression) {
        this.targetExpression = targetExpression;
    }

    public EvalSymbol getConditionalExpression() {
        return conditionalExpression;
    }

    public void setConditionalExpression(EvalSymbol conditionalExpression) {
        this.conditionalExpression = conditionalExpression;
    }

}
