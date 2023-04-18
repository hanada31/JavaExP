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



    public RefinedCondition() {
    }

    public RefinedCondition(ConditionWithValueSet conditionWithValueSet, Value leftVar, String operator, Value rightValue, Unit unit) {
        this.conditionWithValueSet = conditionWithValueSet;
        this.leftVar = leftVar;
        this.leftStr = leftVar == null?"":leftVar.toString();
        this.operator = operator;
        this.rightValue = rightValue;
        this.rightStr= rightValue == null?"":rightValue.toString();
        this.unit = unit;
    }

    @Override
    protected RefinedCondition clone() throws CloneNotSupportedException {
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
        if(leftVar instanceof ParameterRef)
            leftStr = "parameter" + ((ParameterRef)leftVar).getIndex();
        if(rightValue instanceof ParameterRef)
            rightStr = "parameter" + ((ParameterRef)rightValue).getIndex();
        String str = "RefinedCondition: " + leftStr  +" "+ operator  +" "+ rightStr +", which is " +conditionWithValueSet.getIsSatisfy();
        str = str.replace("is not null, which is false", "is null, which is true");
        str = str.replace("is null, which is false", "is not null, which is true");
        return str;
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
