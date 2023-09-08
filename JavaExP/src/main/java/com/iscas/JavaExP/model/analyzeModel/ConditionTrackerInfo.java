package com.iscas.JavaExP.model.analyzeModel;

import com.iscas.JavaExP.utils.PrintUtils;
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
    private final LinkedHashMap<Unit, ConditionWithValueSet> ConditionWithValueSetMap;
    private List<Unit> conditionUnits;
    private List<Unit> tracedUnits;
    private List<Value> caughtValues;


    private ConditionWithValueSet keyConditionWithValueSet;
    private Map<String, List<Integer>> callerOfSingnlar2SourceVar;

    public ConditionTrackerInfo(SootMethod sootMethod, Unit unit) {
        this.sootMethod = sootMethod;
        this.unit = unit;
        this.conditions = new ArrayList<>();
        this.ConditionWithValueSetMap = new LinkedHashMap<>();
        this.caughtValues = new ArrayList<>();
        this.conditionUnits = new ArrayList<>();
        this.tracedUnits = new ArrayList<>();
        this.callerOfSingnlar2SourceVar = new HashMap<String, List<Integer>>();
    }


    public Unit getUnit() {
        return unit;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    public List<Unit> getTracedUnits() {
        return tracedUnits;
    }

    public void setTracedUnits(List<Unit> tracedUnits) {
        this.tracedUnits = tracedUnits;
    }


    public List<Value> getCaughtValues() {
        return caughtValues;
    }
    public void addCaughtedValues(Value v) {
        if(!caughtValues.contains(v))
            caughtValues.add(v);
    }

    public void setCaughtValues(List<Value> caughtValues) {
        this.caughtValues = caughtValues;
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


    public Map<String, List<Integer>> getCallerOfSingnlar2SourceVar() {
        return callerOfSingnlar2SourceVar;
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

    public Map<Unit, ConditionWithValueSet> getConditionWithValueSetMap() {
        return ConditionWithValueSetMap;
    }


    public void addConditionWithValueSet(ConditionWithValueSet conditionWithValueSet) {
        if(!ConditionWithValueSetMap.keySet().contains(conditionWithValueSet.getConditionUnit()))
            ConditionWithValueSetMap.put(conditionWithValueSet.getConditionUnit(), conditionWithValueSet);
    }

    public void addConditionWithValueSetNotLast(ConditionWithValueSet conditionWithValueSet) {
        //TODO
        ConditionWithValueSet last = null;
        List res = new ArrayList(ConditionWithValueSetMap.values());
        if(res.size()>0){
            last = (ConditionWithValueSet) res.get(ConditionWithValueSetMap.size()-1);
        }
        if(!ConditionWithValueSetMap.keySet().contains(conditionWithValueSet.getConditionUnit()))
            ConditionWithValueSetMap.put(conditionWithValueSet.getConditionUnit(), conditionWithValueSet);
        if(last!=null){
            ConditionWithValueSetMap.remove(last.getConditionUnit(),last);
            ConditionWithValueSetMap.put(last.getConditionUnit(), last);
        }
    }
    public ConditionWithValueSet getKeyConditionWithValueSet() {
        List res = new ArrayList(ConditionWithValueSetMap.values());
        if(res.size()>0){
            return (ConditionWithValueSet) res.get(ConditionWithValueSetMap.size()-1);
        }
        return keyConditionWithValueSet;
    }

    public void setKeyConditionWithValueSet(ConditionWithValueSet keyConditionWithValueSet) {
        this.keyConditionWithValueSet = keyConditionWithValueSet;
    }

    @Override
    public String toString() {
        return "ConditionTrackerInfo{" +
                "sootMethod=" + sootMethod.getSignature() +
                ", unit=" + unit +
                ", conditions=" + conditions +
                ", ConditionWithValueSetMap=" + PrintUtils.printMap(ConditionWithValueSetMap) +
                ", keyConditionWithValueSet=" + keyConditionWithValueSet +
                '}';
    }
}
