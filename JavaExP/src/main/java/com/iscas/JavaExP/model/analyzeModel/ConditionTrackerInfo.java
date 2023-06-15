package com.iscas.JavaExP.model.analyzeModel;

import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.StringConstant;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/18 10:33
 * @Version 1.0
 */
public class ConditionTrackerInfo implements Cloneable{
    private SootMethod sootMethod;
    private Unit unit;

    private final List<Value> conditions;
    private final Map<Unit, ConditionWithValueSet> refinedConditions;
    private final List<Value> relatedParamValues;
    private final List<String> relatedParamValuesInStr;
    private final List<String> relatedParamIdsInStr;
    private Set<Integer> relatedValueIndex;
    private final List<SootField> relatedFieldValues;
    private final List<String> relatedFieldValuesInStr;

    private List<Value> caughtValues;
    private List<RelatedMethod> relatedMethodsInSameClass;
    private List<RelatedMethod> relatedMethodsInDiffClass;
    private List<String> relatedMethods;
    private final Map<Integer, ArrayList<RelatedMethod>> relatedMethodsInSameClassMap;
    private final Map<Integer, ArrayList<RelatedMethod>> relatedMethodsInDiffClassMap;

    private List<Unit> conditionUnits;
    private List<Unit> tracedUnits;


    private RelatedVarType relatedVarType;
    private RelatedCondType relatedCondType;
    private Map<String, List<Integer>> callerOfSingnlar2SourceVar;
    public int keyAPISameClassNum;
    public int keyAPIDiffClassNum;


    public ConditionTrackerInfo(SootMethod sootMethod, Unit unit) {
        this.sootMethod = sootMethod;
        this.unit = unit;
        this.conditions = new ArrayList<>();
        this.refinedConditions = new LinkedHashMap<>();

        this.relatedParamValues = new ArrayList<>();
        this.relatedFieldValues = new ArrayList<>();
        this.relatedParamValuesInStr = new ArrayList<>();
        this.relatedParamIdsInStr = new ArrayList<>();
        this.relatedFieldValuesInStr = new ArrayList<>();
        this.relatedValueIndex = new HashSet<>();

        this.caughtValues = new ArrayList<>();
        this.relatedMethodsInSameClass = new ArrayList<>();
        this.relatedMethodsInDiffClass = new ArrayList<>();
        this.relatedMethods = new ArrayList<>();

        this.conditionUnits = new ArrayList<>();
        this.tracedUnits = new ArrayList<>();

        this.relatedMethodsInSameClassMap = new TreeMap<Integer, ArrayList<RelatedMethod>>();
        this.relatedMethodsInDiffClassMap = new TreeMap<Integer, ArrayList<RelatedMethod>>();
        this.relatedCondType = RelatedCondType.Empty;
        this.relatedVarType = RelatedVarType.Unknown;
        this.callerOfSingnlar2SourceVar = new HashMap<String, List<Integer>>();
    }


    public Unit getUnit() {
        return unit;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public void setRelatedCondType(RelatedCondType relatedCondType) {
        this.relatedCondType = relatedCondType;
    }

    public RelatedVarType getRelatedVarType() {
        if (isParameterOnly()) return RelatedVarType.Parameter;
        if (isFieldOnly()) return RelatedVarType.Field;
        if (isParaAndField()) return RelatedVarType.ParaAndField;
        if (isEmpty()) return RelatedVarType.Empty;
        return relatedVarType;
    }
    public boolean isEmpty() {
        return getRelatedMethods().size() == 0 && getConditions().size() == 0 && caughtValues.isEmpty();
    }

    public boolean isParameterOnly() {
        return getRelatedParamValues().size() > 0 && getRelatedFieldValues().size() == 0;
    }

    public boolean isFieldOnly() {
        return getRelatedParamValues().size() == 0 && getRelatedFieldValues().size() > 0;
    }

    public boolean isParaAndField() {
        return getRelatedParamValues().size() > 0 && getRelatedFieldValues().size() > 0;
    }

    public RelatedCondType getRelatedCondType() {
        if(getRefinedConditions().size()==0)
            relatedCondType = RelatedCondType.Empty;
        else
            relatedCondType = RelatedCondType.Basic;
        return relatedCondType;
    }


    public List<Unit> getTracedUnits() {
        return tracedUnits;
    }

    public void setTracedUnits(List<Unit> tracedUnits) {
        this.tracedUnits = tracedUnits;
    }


    public void addRelatedMethodsInSameClassMap(RelatedMethod m) {
        if(!relatedMethodsInSameClassMap.containsKey(m.getDepth()))
            relatedMethodsInSameClassMap.put(m.getDepth(), new ArrayList<>());
        if(!relatedMethods.contains(m.getMethod())) {
            for(RelatedMethod temp : relatedMethodsInSameClassMap.get(m.getDepth())){
                if(temp.toString().equals(m.toString()))
                    return;
            }
            relatedMethodsInSameClassMap.get(m.getDepth()).add(m);
            if(m.getSource() == RelatedMethodSource.FIELD || m.getSource() == RelatedMethodSource.FIELDCALLER)
                keyAPISameClassNum++;
        }
    }

    public void addRelatedMethodsInDiffClassMap(RelatedMethod m) {
        if(!relatedMethodsInDiffClassMap.containsKey(m.getDepth()))
            relatedMethodsInDiffClassMap.put(m.getDepth(), new ArrayList<>());
        if(!relatedMethods.contains(m.getMethod())){
            for(RelatedMethod temp : relatedMethodsInDiffClassMap.get(m.getDepth())){
                if(temp.toString().equals(m.toString()))
                    return;
            }
            relatedMethodsInDiffClassMap.get(m.getDepth()).add(m);
            if(m.getSource() == RelatedMethodSource.FIELD || m.getSource() == RelatedMethodSource.FIELDCALLER)
                keyAPIDiffClassNum++;
        }
    }

    public List<RelatedMethod> getRelatedMethodsInSameClass(boolean compute) {
        if(!compute) return  relatedMethodsInSameClass;
        for(Integer depth:relatedMethodsInSameClassMap.keySet()) {
            for (RelatedMethod relatedMethod : relatedMethodsInSameClassMap.get(depth)) {
                addRelatedMethodsInSameClass(relatedMethod);
            }
        }
        return relatedMethodsInSameClass;
    }
    public List<RelatedMethod> getRelatedMethodsInDiffClass(boolean compute) {
        if(!compute) return  relatedMethodsInDiffClass;
        for(Integer depth:relatedMethodsInDiffClassMap.keySet()) {
            for (RelatedMethod relatedMethod : relatedMethodsInDiffClassMap.get(depth)) {
                addRelatedMethodsInDiffClass(relatedMethod);
            }
        }
        return relatedMethodsInDiffClass;
    }
    public void addRelatedMethodsInSameClass(RelatedMethod m) {
        if(!relatedMethodsInSameClass.contains(m))
            relatedMethodsInSameClass.add(m);
    }
    public void addRelatedMethodsInDiffClass(RelatedMethod m) {
        if(!relatedMethodsInDiffClass.contains(m))
            relatedMethodsInDiffClass.add(m);
    }


    public List<Value> getCaughtValues() {
        return caughtValues;
    }
    public void addCaughtedValues(Value v) {
        if(!caughtValues.contains(v))
            caughtValues.add(v);
    }

    public List<String> getRelatedMethods() {
        return relatedMethods;
    }

    public void setCaughtValues(List<Value> caughtValues) {
        this.caughtValues = caughtValues;
    }

    public void setRelatedMethodsInSameClass(List<RelatedMethod> relatedMethodsInSameClass) {
        this.relatedMethodsInSameClass = relatedMethodsInSameClass;
    }

    public void setRelatedMethodsInDiffClass(List<RelatedMethod> relatedMethodsInDiffClass) {
        this.relatedMethodsInDiffClass = relatedMethodsInDiffClass;
    }

    public void setRelatedMethods(List<String> relatedMethods) {
        this.relatedMethods = relatedMethods;
    }

    public void setConditions(String conditions) {
        if(conditions == null) return;
        conditions= conditions.replace("\"","");
        conditions= conditions.replace("[","").replace("]","");
        conditions= conditions.replace("at ","");
        String[] ss = conditions.split(", ");
        for(String t: ss){
            this.conditions.add(StringConstant.v(t));
        }

    }

    public List<Unit> getConditionUnits() {
        return conditionUnits;
    }

    public void setConditionUnits(List<Unit> conditionUnits) {
        this.conditionUnits = conditionUnits;
    }

    public void addRelatedMethods(String signature) {
//        if (SootUtils.getSootMethodBySignature(signature) != null) {
//            addRelatedMethods(SootUtils.getSootMethodBySignature(signature), signature);
//        } else
        if (!relatedMethods.contains(signature))
            relatedMethods.add(signature);
    }

    public Map<String, List<Integer>> getCallerOfSingnlar2SourceVar() {
        return callerOfSingnlar2SourceVar;
    }

    public void setCallerOfSingnlar2SourceVar(Map<String, List<Integer>> callerOfSingnlar2SourceVar) {
        this.callerOfSingnlar2SourceVar = callerOfSingnlar2SourceVar;
    }

    public void addCallerOfSingnlar2SourceVar(String method, int sourceId ) {
        if(callerOfSingnlar2SourceVar.containsKey(method)){
            if(callerOfSingnlar2SourceVar.get(method).contains(sourceId)){
                return;
            }
        }else{
            callerOfSingnlar2SourceVar.put(method, new ArrayList<>());
        }
        callerOfSingnlar2SourceVar.get(method).add(sourceId);

    }

    public List<Value> getConditions() {
        return conditions;
    }
    public void addRelatedCondition(Value condition) {
        if(!conditions.contains(condition))
            conditions.add(condition);
    }

    public Map<Unit, ConditionWithValueSet> getRefinedConditions() {
        return refinedConditions;
    }

    public ConditionWithValueSet getRefinedConditionWithKey(Unit unit) {
        return refinedConditions.get(unit);
    }

    public void addRefinedConditions(ConditionWithValueSet conditionWithValueSet) {
        if(!refinedConditions.keySet().contains(conditionWithValueSet.getConditionUnit()))
            refinedConditions.put(conditionWithValueSet.getConditionUnit(), conditionWithValueSet);
    }

    public List<SootField> getRelatedFieldValues() {
        return relatedFieldValues;
    }
    public void addRelatedFieldValues(SootField v) {
        if(!relatedFieldValues.contains(v))
            relatedFieldValues.add(v);
    }

    public Set<Integer> getRelatedValueIndex() {
        return relatedValueIndex;
    }
    public List<String> getRelatedParamValuesInStr() {
        return relatedParamValuesInStr;
    }
    public List<String> getRelatedParamIdsInStr() {
        return relatedParamIdsInStr;
    }
    public List<String> getRelatedFieldValuesInStr() {
        return relatedFieldValuesInStr;
    }

    public void setRelatedParamValuesInStr(String relatedParamValues) {
        if(relatedParamValues == null) return;
        relatedParamValues= relatedParamValues.replace("<","");
        relatedParamValues= relatedParamValues.replace(">","");
        String[] ss = relatedParamValues.split(", ");
        for(String t: ss){
            if(t.contains(": "))
                this.relatedParamValuesInStr.add(t.split(": ")[1]);
            this.relatedParamIdsInStr.add(t.split(": ")[0].replace("@parameter",""));
        }
    }

    public void setRelatedFieldValuesInStr(String relatedFieldValues) {
        if(relatedFieldValues == null) return;
        relatedFieldValues= relatedFieldValues.replace("<","");
        relatedFieldValues= relatedFieldValues.replace(">","");
        String[] ss = relatedFieldValues.split(", ");
        for(String t: ss){
            this.relatedFieldValuesInStr.add(t);
        }
    }

    public List<Value> getRelatedParamValues() {
        return relatedParamValues;
    }
    public void addRelatedParamValue(Value v) {
        if(!relatedParamValues.contains(v))
            relatedParamValues.add(v);
    }

//    @Override
//    protected Object clone() throws CloneNotSupportedException {
//        ConditionTrackerInfo conditionTrackerInfo = new ConditionTrackerInfo();
//
//        return conditionTrackerInfo;
//    }
}
