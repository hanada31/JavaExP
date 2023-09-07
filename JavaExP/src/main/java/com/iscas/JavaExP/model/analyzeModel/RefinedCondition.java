package com.iscas.JavaExP.model.analyzeModel;

import soot.Unit;
import soot.Value;
import soot.jimple.ParameterRef;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author hanada
 * @Date 2023/4/14 10:19
 * @Version 1.0
 */
public class RefinedCondition implements Cloneable {
    private Value leftVar;
    private RefinedCondition leftRefinedCondition;
    private RefinedCondition rightRefinedCondition;
    private String leftStr;
    private String operator;
    private Value rightValue;
    private String rightStr;
    private ConditionWithValueSet conditionWithValueSet;
    private Unit unit;
    private String refinedConditionInCaller;


    private boolean satisfied = true;

    public RefinedCondition(ConditionWithValueSet conditionWithValueSet, Value leftVar, String operator, Value rightValue, Unit unit, boolean satisfied) {
        this.conditionWithValueSet = conditionWithValueSet;
        this.satisfied = satisfied;
        this.leftVar = leftVar;
        this.leftStr = leftVar == null?"":leftVar.toString();
        this.operator = operator;
        this.rightValue = rightValue;
        this.rightStr= rightValue == null?"":rightValue.toString();
        this.unit = unit;
    }

    public RefinedCondition(String refinedConditionInCaller) {
        this.refinedConditionInCaller = refinedConditionInCaller;
        for(String op: IROperator.operators){
            if(refinedConditionInCaller.contains(" " + op +" ")){
                String ss[] = refinedConditionInCaller.split(" " + op +" ");
                if(ss.length == 2){
                    this.operator = op;
                    this.leftStr = ss[0];
                    this.rightStr = ss[1];
                }
            }
        }


    }

    public RefinedCondition() {

    }

    public RefinedCondition getLeftRefinedCondition() {
        return leftRefinedCondition;
    }

    public void setLeftRefinedCondition(RefinedCondition leftRefinedCondition) {
        this.leftRefinedCondition = leftRefinedCondition;
    }

    public RefinedCondition getRightRefinedCondition() {
        return rightRefinedCondition;
    }

    public void setRightRefinedCondition(RefinedCondition rightRefinedCondition) {
        this.rightRefinedCondition = rightRefinedCondition;
    }

    public void changeSatisfied(){
        if(this.satisfied)
            this.satisfied = false;
        else
            this.satisfied = true;
    }
    public boolean isSatisfied() {
        return this.satisfied;
    }
    public void setSatisfied(boolean flag) {
        this.satisfied = flag;
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
        if(operator==null && refinedConditionInCaller!= null) return refinedConditionInCaller;
        if(operator==null) return "";
        if(satisfied == false) {
            if(operator.equals(IROperator.isOP)) {
                if(rightStr.contains("not"))
                    rightStr.replace("not","");
                else
                    operator =IROperator.isNotOP;
                satisfied = true;
            }else if(operator.equals(IROperator.isNotOP)) {
                operator =IROperator.isOP;
                satisfied = true;
            }else if(operator.equals(IROperator.largerOrEqualOP)) {
                operator =IROperator.smallerOP;
                satisfied = true;
            }else if(operator.equals(IROperator.smallerOrEqualOP)) {
                operator =IROperator.largerOP;
                satisfied = true;
            }else if(operator.equals(IROperator.smallerOP)) {
                operator =IROperator.largerOrEqualOP;
                satisfied = true;
            }else if(operator.equals(IROperator.largerOP)) {
                operator =IROperator.smallerOrEqualOP;
                satisfied = true;
            }else if(operator.equals(IROperator.equalsOp) ) {
                if(rightStr.contains("not"))
                    rightStr.replace("not","");
                else
                    operator =IROperator.notEqualsOp;
                satisfied = true;
            }else if(operator.equals(IROperator.notEqualsOp)) {
                operator =IROperator.equalsOp;
                satisfied = true;
            }else if(operator.equals(IROperator.startsWithOP)) {
                if(rightStr.contains("not"))
                    rightStr.replace("not","");
                else
                    operator =IROperator.notStartsWithOP;
                satisfied = true;
            }else if(operator.equals(IROperator.endsWithOP)) {
                if(rightStr.contains("not"))
                    rightStr.replace("not","");
                else
                    operator =IROperator.notEndsWithOP;
                satisfied = true;
            }else if(operator.equals(IROperator.notStartsWithOP)) {
                operator =IROperator.startsWithOP;
                satisfied = true;
            }else if(operator.equals(IROperator.notEndsWithOP)) {
                operator =IROperator.endsWithOP;
                satisfied = true;
            }
            else if(operator.equals(IROperator.notContainsOP)) {
                operator =IROperator.containsOP;
                satisfied = true;
            }else if(operator.equals(IROperator.containsOP)) {
                operator =IROperator.notContainsOP;
                satisfied = true;
            }
        }

        String str = leftStr  +" "+ operator  +" "+ rightStr ;
        str = processString(str);
        return satisfied?str:str+", it returns 0";
    }

    public static String processString(String input) {
        // 匹配 "parameterX cmp YL" 形式的字符串
        String regex = "parameter(\\d+) cmp (\\d+)L (larger or equal| smaller or equal|larger than|smaller than|equal) 0";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            // 提取参数名、数字、比较符
            String paramName = "parameter" + matcher.group(1);
            String cmpValue = matcher.group(2);
            String comparison = matcher.group(3);
            // 根据比较符构建新的字符串
            String replacement = paramName + " " + comparison + " " +  cmpValue;

            // 将匹配的部分替换为新字符串
            matcher.appendReplacement(result, replacement);
        }

        // 将剩余部分添加到结果中
        matcher.appendTail(result);

        return result.toString();
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
