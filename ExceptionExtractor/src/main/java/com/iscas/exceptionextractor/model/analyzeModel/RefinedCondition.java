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
    public void setSatisfied(boolean flag) {
        satisfied = flag;
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
        refinedCondition.setSatisfied(satisfied);
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
        if(rightValue instanceof ParameterRef)
            this.rightStr = "parameter" + ((ParameterRef)rightValue).getIndex();
        return rightStr;
    }

    public void setRightStr(String rightStr) {
        this.rightValue = null;
        this.rightStr = rightStr;
    }

    @Override
    public String toString() {
        if(refinedConditionInCaller!= null) return refinedConditionInCaller;
        if(operator==null) return "";
        boolean satisfied2 = satisfied;
        if(satisfied == false) {
            if(operator.equals("is")) {
                operator ="is not";
                satisfied2 = true;
            }else if(operator.equals("is not")) {
                operator ="is";
                satisfied2 = true;
            }else if(operator.equals("is null")) {
                operator ="is not null";
                satisfied2 = true;
            }else if(operator.equals("larger or equal")) {
                operator ="smaller than";
                satisfied2 = true;
            }else if(operator.equals("smaller or equal")) {
                operator ="larger than";
                satisfied2 = true;
            }else if(operator.equals("smaller than")) {
                operator ="larger or equal";
                satisfied2 = true;
            }else if(operator.equals("larger than")) {
                operator ="smaller or equal";
                satisfied2 = true;
            }else if(operator.equals("equals") ) {
                operator ="not equals";
                satisfied2 = true;
            }else if(operator.equals("not equals")) {
                operator ="equals";
                satisfied2 = true;
            }else if(operator.equals("startsWith")) {
                operator ="not startsWith";
                satisfied2 = true;
            }else if(operator.equals("endsWith")) {
                operator ="not endsWith";
                satisfied2 = true;
            }else if(operator.equals("not startsWith")) {
                operator ="startsWith";
                satisfied2 = true;
            }else if(operator.equals("not endsWith")) {
                operator ="endsWith";
                satisfied2 = true;
            }
        }
        String str = leftStr  +" "+ operator  +" "+ rightStr ;
        return satisfied2==false? (str+", which is false"): str;
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
