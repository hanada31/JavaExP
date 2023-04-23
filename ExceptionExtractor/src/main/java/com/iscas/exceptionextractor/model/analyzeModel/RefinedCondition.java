package com.iscas.exceptionextractor.model.analyzeModel;

import soot.Unit;
import soot.Value;
import soot.jimple.ParameterRef;

/**
 * @Author hanada
 * @Date 2023/4/14 10:19
 * @Version 1.0
 */
public class RefinedCondition implements Cloneable {
    private Value leftVar;
    private String leftStr;
    private String operator;
    private Value rightValue;
    private String rightStr;
    private ConditionWithValueSet conditionWithValueSet;
    private Unit unit;
    private String refinedConditionInCaller;


    private boolean satisfied;

    public RefinedCondition() {
    }

    public RefinedCondition(ConditionWithValueSet conditionWithValueSet, Value leftVar, String operator, Value rightValue, Unit unit) {
        this.conditionWithValueSet = conditionWithValueSet;
        this.satisfied = conditionWithValueSet.getIsSatisfy();
        this.leftVar = leftVar;
        this.leftStr = leftVar == null?"":leftVar.toString();
        this.operator = operator;
        this.rightValue = rightValue;
        this.rightStr= rightValue == null?"":rightValue.toString();
        this.unit = unit;
    }

    public RefinedCondition(String refinedConditionInCaller) {
        this.refinedConditionInCaller = refinedConditionInCaller;
    }
    public void changeSatisfied(){
        if(satisfied) satisfied = false;
        else  satisfied = true;
    }
    public boolean isSatisfied() {
        return satisfied;
    }
    @Override
    public RefinedCondition clone() throws CloneNotSupportedException {
        RefinedCondition refinedCondition = new RefinedCondition();
        refinedCondition.setLeftVar(leftVar);
        refinedCondition.setRightValue(rightValue);
        refinedCondition.setOperator(operator);
        refinedCondition.setLeftStr(leftStr);
        refinedCondition.setRightStr(rightStr);
        refinedCondition.setUnit(unit);
        refinedCondition.setConditionWithValueSet(conditionWithValueSet);
        return refinedCondition;
    }

    public Value getLeftVar() {
        return leftVar;
    }

    public void setLeftVar(Value leftVar) {
        this.leftVar = leftVar;
        this.leftStr = leftVar == null?"":leftVar.toString();

        if(leftVar instanceof ParameterRef)
            leftStr = "parameter" + ((ParameterRef)leftVar).getIndex();
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Value getRightValue() {
        return rightValue;
    }

    public void setRightValue(Value rightValue) {
        this.rightValue = rightValue;
        this.rightStr= rightValue == null?"":rightValue.toString();
        if(rightValue instanceof ParameterRef)
            rightStr = "parameter" + ((ParameterRef)rightValue).getIndex();
    }
    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public String getLeftStr() {
        return leftStr;
    }

    public void setLeftStr(String leftStr) {
        this.leftVar = null;
        this.leftStr = leftStr;
    }

    public String getRightStr() {
        return rightStr;
    }

    public void setRightStr(String rightStr) {
        this.rightValue = null;
        this.rightStr = rightStr;
    }

    @Override
    public String toString() {
        if(refinedConditionInCaller!= null) return refinedConditionInCaller;
        String str = "RefinedCondition: " + leftStr  +" "+ operator  +" "+ rightStr ;
        boolean satisfied2 = satisfied;
        if(satisfied == false) {
            if(str.contains("is not")) {
                str = str.replace("is not", "is");
                satisfied2 = true;
            }else if(str.contains("is null")) {
                str = str.replace("is null", "is not null");
                satisfied2 = true;
            }else if(str.contains("larger or equal")) {
                str = str.replace("larger or equal", "smaller than");
                satisfied2 = true;
            }else if(str.contains("smaller or equal")) {
                str = str.replace("smaller or equal", "larger than");
                satisfied2 = true;
            }else if(str.contains("smaller or equal")) {
                str = str.replace("smaller or equal", "larger than");
                satisfied2 = true;
            }
        }
        return str+", which is " +satisfied2;
    }

    public Unit getUnit() {
        return unit;
    }


    public ConditionWithValueSet getConditionWithValueSet() {
        return conditionWithValueSet;
    }

    public void setConditionWithValueSet(ConditionWithValueSet conditionWithValueSet) {
        this.conditionWithValueSet = conditionWithValueSet;
    }
}
