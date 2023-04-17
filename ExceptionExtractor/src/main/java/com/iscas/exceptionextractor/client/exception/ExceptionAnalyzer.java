package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.model.analyzeModel.ConditionWithValueSet;
import com.iscas.exceptionextractor.model.analyzeModel.RefinedCondition;
import com.iscas.exceptionextractor.utils.ConstantUtils;
import com.iscas.exceptionextractor.utils.FileUtils;
import com.iscas.exceptionextractor.utils.SootUtils;
import com.iscas.exceptionextractor.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;

/**
 * @Author hanada
 * @Date 2022/3/11 15:21
 * @Version 1.0
 */
@Slf4j
public class ExceptionAnalyzer extends Analyzer {
    List<ExceptionInfo> declaredExceptionInfoList;
    List<ExceptionInfo> caughtExceptionInfoList;
    List<ExceptionInfo> thrownExceptionInfoList;
    Set<String> permissionSet = new HashSet<>();
    Set<String> nonThrowUnits = new HashSet<>();
    boolean openFilter = false;
    public ExceptionAnalyzer() {
        super();
    }

    /**
     * true: not analyze
     * @param sootMethod
     * @return
     */
    private boolean filterMethod(SootMethod sootMethod) {
        List<String> mtds = new ArrayList<>();
        mtds.add("throw_with_outVar_condition(");
        for(String tag: mtds){
            if (sootMethod.getSignature().contains(tag)) {
                return false;
            }
        }
        return true;
    }

    private void outputExceptionConditions(SootMethod sootMethod, ExceptionInfo exceptionInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(sootMethod.getSignature()+"\n");
        sb.append(exceptionInfo.getExceptionName()+"\n");
        int size = sb.length();
        for(ConditionWithValueSet conditionWithValueSet :exceptionInfo.getRefinedConditions().values() ){
            if(conditionWithValueSet.toString().length()>0)
                sb.append(conditionWithValueSet+"\n");
        }
        if(exceptionInfo.getRelatedVarType() == RelatedVarType.Empty){
            sb.append("RefinedCondition: no condition\n");
        }
        if(size < sb.length())
            FileUtils.writeText2File(MyConfig.getInstance().getResultFolder()+"out.txt", sb.toString()+"\n",true);

    }

    @Override
    public void analyze() {
        getDeclaredExceptionList();
        getThrownExceptionList();
        getCaughtExceptionList();
    }

    /**
     * analyze the information of declared exceptions
     */
    private void getDeclaredExceptionList() {
        log.info("getDeclaredExceptionList start...");
        declaredExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : getApplicationClasses()) {
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setExceptionName(sootClass.getName());
            if(!exceptionInfo.findExceptionType(sootClass)) continue;
            declaredExceptionInfoList.add(exceptionInfo);
        }
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfDeclaredException(declaredExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"declaredException");
        log.info("getDeclaredExceptionList end...");
    }

    /**
     * analyze the information of caught exceptions
     */
    private void getCaughtExceptionList() {
        log.info("getCaughtExceptionList start...");
        caughtExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(openFilter && filterMethod(sootMethod)) continue;
                if (sootMethod.hasActiveBody()) {
                    try {
                        getCaughtUnitWithType(sootMethod,new  HashSet<>());
                    } catch (Exception |  Error e) {
                        log.info("Exception |  Error:::" + sootMethod.getSignature());
                        e.printStackTrace();
                    }
                }
            }
        }
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfCaughtException(caughtExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"caughtException");
        log.info("getCaughtExceptionList end...");
    }

    /**
     * analyze the information of thrown exceptions
     */
    private void getThrownExceptionList() {
        log.info("getThrownExceptionList start...");
        thrownExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(openFilter && filterMethod(sootMethod)) continue;
                if (sootMethod.hasActiveBody()) {
                    Map<SootMethod, Map<Unit, Local>> method2unit2Value = getThrowUnitWithValue(sootMethod,new  HashSet<>());
                    for(Map.Entry<SootMethod, Map<Unit,Local>> entryOuter: method2unit2Value.entrySet()) {
                        SootMethod sm =entryOuter.getKey();
                        Map<Unit,Local> unit2Value = entryOuter.getValue();
                        Map<Unit, SootClass> unit2Type = new HashMap<>();
                        for (Map.Entry<Unit, Local> entryInner : unit2Value.entrySet()) {
                            getThrowUnitWithType(unit2Type, sm, entryInner.getKey(), entryInner.getValue());
                        }
                        for (Map.Entry<Unit, SootClass> entryInner : unit2Type.entrySet()) {
                            createNewExceptionInfo(sootMethod, sm, entryInner.getKey(), entryInner.getValue());
                        }
                    }
                }
            }
            ExceptionInfoClientOutput.writeThrownExceptionInJsonForCurrentClass(sootClass, thrownExceptionInfoList);
        }
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException");
        exceptionListElement.clear();
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException2(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException2");

        log.info("getThrownExceptionList end...");
    }

    /**
     * get catch units with value from a method
     * @return
     */
    public Map<SootMethod, Map<Unit, SootClass>> getCaughtUnitWithType(SootMethod sootMethod, Set<SootMethod> history){
        Map<SootMethod, Map<Unit, SootClass>> method2unit2Value = new HashMap<>();
        if(!sootMethod.hasActiveBody() || history.contains(sootMethod)) return method2unit2Value;
        history.add(sootMethod);
        Set<Unit> labelHistory = new HashSet();
        for (Trap trap : sootMethod.getActiveBody().getTraps()) {
            if(trap.getException().getName().equals("java.lang.Throwable")) continue;
            if(labelHistory.contains(trap.getHandlerUnit()))
                continue;
            labelHistory.add(trap.getHandlerUnit());
            ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, trap,  trap.getException().getName());
            exceptionInfo.findExceptionType(trap.getException());
            caughtExceptionInfoList.add(exceptionInfo);
        }
        return  method2unit2Value;
    }

    /**
     * get throw units with value from a method
     * @return
     */
    public Map<SootMethod, Map<Unit, Local>> getThrowUnitWithValue(SootMethod sootMethod, Set<SootMethod> history){
        Map<SootMethod, Map<Unit, Local>> method2unit2Value = new HashMap<>();
        if(!sootMethod.hasActiveBody() || history.contains(sootMethod)) return method2unit2Value;
        history.add(sootMethod);

        for (Unit unit : sootMethod.getActiveBody().getUnits()) {
            if (unit instanceof ThrowStmt) {
                ThrowStmt throwStmt = (ThrowStmt) unit;
                Value throwValue = throwStmt.getOp();
                if (throwValue instanceof Local) {
                    addThrowPoint(method2unit2Value, sootMethod, unit, throwValue);
                }
            }else {
                getOtherNonThrowUnits(method2unit2Value, sootMethod, unit, history);
            }
        }
        return  method2unit2Value;
    }


    /**
     * except throwUnit, find out more methods that operate an exception
     * @param method2unit2Value
     * @param sootMethod
     * @param unit
     * @param history
     */
    private void getOtherNonThrowUnits(Map<SootMethod, Map<Unit, Local>> method2unit2Value, SootMethod sootMethod, Unit unit, Set<SootMethod> history) {

        boolean find = false;
        //parameter is throwable, inline the callee
        InvokeExpr invoke = SootUtils.getInvokeExp(unit);
        if(invoke==null || ((InvokeExpr) invoke).getMethod() == null ) return;
        if(invoke.getMethod().getName().contains("access$") || invoke.getMethod().getName().contains("<init>")) return;
        if(!invoke.getMethod().getDeclaringClass().getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX)) return;
        for(Value arg : invoke.getArgs()) {
            if (arg.getType().toString().endsWith("Throwable") || arg.getType().toString().endsWith("Exception")) {
                Value throwValue = arg;
                if (throwValue instanceof Local) {
                    List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),throwValue, unit);
                    if(defsOfOps.size()>0) {
                        Unit defOfLocal = defsOfOps.get(0);
                        if (((AbstractDefinitionStmt) defOfLocal).getRightOp() instanceof JNewExpr) {
                            addThrowPoint(method2unit2Value, sootMethod, unit, throwValue);
                            //                    method2unit2Value.putAll(getThrowUnitWithValue(invoke.getMethod(), history));
                            nonThrowUnits.add("parameter (rethrow or log)\t" + invoke.getMethod().getSignature());
                            find = true;
                        }
                    }
                }
            }
        }
        //base is throwable, inline the callee, exception.rethrow
        //all of them are caught exception, but no new exception
        //can be removed
        if(!find & invoke instanceof AbstractInstanceInvokeExpr) {
            Value base = ((AbstractInstanceInvokeExpr) invoke).getBase();
            if(base.getType().toString().endsWith("Throwable") || base.getType().toString().endsWith("Exception")){
                List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),base, unit);
                Unit defOfLocal = defsOfOps.get(0);
                if(defOfLocal instanceof  JNewExpr) {
                    for (Unit temp : SootUtils.getUnitListFromMethod(invoke.getMethod())) {
                        if (temp instanceof ThrowStmt) {
                            addThrowPoint(method2unit2Value, sootMethod, unit, base);
//                        method2unit2Value.putAll(getThrowUnitWithValue(invoke.getMethod(), history));
                            nonThrowUnits.add("base (rethrow or log)\t" + invoke.getMethod().getSignature());
                            find = true;
                            break;
                        }
                    }
                }
            }
        }
    }


    private void addThrowPoint(Map<SootMethod, Map<Unit, Local>> method2unit2Value, SootMethod sootMethod, Unit unit, Value throwValue) {
        if (!method2unit2Value.containsKey(sootMethod))
            method2unit2Value.put(sootMethod, new HashMap<>());
        method2unit2Value.get(sootMethod).put(unit, (Local) throwValue);
    }

    /**
     * get throw units with message from a method
     */
    public void getThrowUnitWithType(Map<Unit, SootClass> unit2Message, SootMethod sootMethod, Unit unit, Local localTemp){
        List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),localTemp, unit);
        if (defsOfOps.size() == 0) return;
        Unit defOfLocal = defsOfOps.get(0);
        if (defOfLocal.equals(unit)) return;

        if (defOfLocal instanceof DefinitionStmt) {
            Value rightValue = ((DefinitionStmt)defOfLocal).getRightOp();
            if (rightValue instanceof NewExpr) {
                NewExpr newRightValue = (NewExpr) rightValue;
                unit2Message.put(unit,newRightValue.getBaseType().getSootClass());
            } else if (rightValue instanceof NewArrayExpr) {
                NewArrayExpr rightValue1 = (NewArrayExpr) rightValue;
                String s = rightValue1.getBaseType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    unit2Message.put(unit,Scene.v().getSootClass(s));
                }
            } else if (rightValue instanceof Local) {
                getThrowUnitWithType(unit2Message, sootMethod, unit, (Local) rightValue);
            } else if (rightValue instanceof JCastExpr) {
                JCastExpr castExpr = (JCastExpr) rightValue;
                String s = castExpr.getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    unit2Message.put(unit,Scene.v().getSootClass(s));
                } else {
                    Value value = castExpr.getOpBox().getValue();
                    if (value instanceof Local) {
                        getThrowUnitWithType(unit2Message, sootMethod, unit, (Local) value);
                    }
                }
            } else if (rightValue instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) rightValue;
                Type returnType = invokeExpr.getMethod().getReturnType();
                if (returnType.toString().endsWith("Exception") || returnType.toString().equals("java.lang.Throwable")) {
                    unit2Message.put(unit,Scene.v().getSootClass(returnType.toString()));
                }

            } else if (rightValue instanceof CaughtExceptionRef) {
                //todo
                //caught an Exception here
                //$r1 := @caughtexception;
            } else if (rightValue instanceof PhiExpr) {
                PhiExpr phiExpr = (PhiExpr) rightValue;
                for (ValueUnitPair arg : phiExpr.getArgs()) {
                    if (arg.getValue() instanceof Local) {
                        getThrowUnitWithType(unit2Message, sootMethod, unit, (Local) arg.getValue());
                    }
                }
            } if (rightValue instanceof FieldRef) {
                FieldRef rightValue1 = (FieldRef) rightValue;
                String s = rightValue1.getField().getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    unit2Message.put(unit,Scene.v().getSootClass(s));
                }
            } else if (rightValue instanceof ParameterRef) {
                ParameterRef rightValue1 = (ParameterRef) rightValue;
                String s = rightValue1.getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    unit2Message.put(unit,Scene.v().getSootClass(s));
                }
            }  else if (rightValue instanceof ArrayRef) {
                ArrayRef rightValue1 = (ArrayRef) rightValue;
                Value value = rightValue1.getBaseBox().getValue();
                if (value instanceof Local) {
                    getThrowUnitWithType(unit2Message, sootMethod, unit, (Local) value);
                }
            }
        }
    }

    /**
     * get the latest condition info for an ExceptionInfo
     * only analyze one level if condition, forward
     */
    private boolean conditionOfRetIsCaughtException(SootMethod sootMethod, Unit unit, HashSet<Unit> units) {
        if(units.contains(unit) || units.size()> ConstantUtils.CONDITIONHISTORYSIZE) return false;
        units.add(unit);
        Body body = sootMethod.getActiveBody();
        ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(body);
        List<Unit> predsOf = unitGraph.getPredsOf(unit);
        for (Unit predUnit : predsOf) {
           if (predUnit instanceof JIdentityStmt ) {
                JIdentityStmt stmt = (JIdentityStmt) predUnit;
                if(stmt.getRightOp() instanceof CaughtExceptionRef){
                    return true;
                }
            }
            boolean flag = conditionOfRetIsCaughtException(sootMethod, predUnit, units);
            if(flag)
                return true;
        }
        return false;
    }
    /**
     * creat a New ExceptionInfo object and add content
     */
    private void createNewExceptionInfo(SootMethod excpetionInfoSootMethod, SootMethod sootMethod, Unit unit, SootClass exceptionName) {
        if(exceptionName.getName().equals("java.lang.Throwable")) return;
        ExceptionInfo exceptionInfo =  new ExceptionInfo(excpetionInfoSootMethod, unit, exceptionName.getName());
        exceptionInfo.findExceptionType(exceptionName);
        getExceptionMessage(sootMethod, unit, exceptionInfo, new ArrayList<>());
        if(exceptionInfo.getExceptionMsg()==null){
            exceptionInfo.setExceptionMsg("[\\s\\S]*");
//            return;
        }
        getConditionandValueFromUnit(sootMethod, unit, exceptionInfo, true);
        int b = exceptionInfo.getConditions().size();

        Set<Unit> retUnits = new HashSet<>();
        for (Unit u: sootMethod.getActiveBody().getUnits()) {
            if (u instanceof ReturnStmt) {
                if(!conditionOfRetIsCaughtException(sootMethod, u, new HashSet<Unit>()))
                    retUnits.add(u);
            }
        }
        for(Unit condUnit: exceptionInfo.getConditionUnits()) {
            getRetUnitsFlowIntoConditionUnits(sootMethod, condUnit, retUnits, new HashSet<Unit>());
        }
        for (Unit retUnit : retUnits) {
            getConditionandValueFromUnit(sootMethod, retUnit, exceptionInfo, false);
        }
        int e = exceptionInfo.getConditions().size();
        getConditionType(exceptionInfo, e-b);
        thrownExceptionInfoList.add(exceptionInfo);
    }

    private void getConditionType(ExceptionInfo exceptionInfo, int retValue) {
        if(retValue>0)
            exceptionInfo.setRelatedCondType(RelatedCondType.NotReturn);
        else if(exceptionInfo.getConditions().size()>0)
            exceptionInfo.setRelatedCondType(RelatedCondType.Basic);
        else
            exceptionInfo.setRelatedCondType(RelatedCondType.Empty);
    }

    private void getRetUnitsFlowIntoConditionUnits(SootMethod sootMethod, Unit unit, Set<Unit> retUnits, HashSet<Unit> history) {
        if(history.contains(unit)) return;
        history.add(unit);
        BriefUnitGraph graph = new BriefUnitGraph(sootMethod.getActiveBody());
        for (Unit u: graph.getSuccsOf(unit)) {
            if(u instanceof ReturnStmt){
                retUnits.remove(u);
            }else{
                getRetUnitsFlowIntoConditionUnits(sootMethod, u, retUnits, history);
            }
        }
    }

    private void getConditionandValueFromUnit(SootMethod sootMethod, Unit unit, ExceptionInfo exceptionInfo, boolean fromThrow) {
        List<String> trace = new ArrayList<>();
        trace.add(sootMethod.getSignature());

        getExceptionCondition(sootMethod, unit, exceptionInfo, new HashSet<>(), fromThrow, null);
        for(ConditionWithValueSet conditionWithValueSet :exceptionInfo.getRefinedConditions().values() ) {
            conditionWithValueSet.optimizeCondition();
        }

        outputExceptionConditions(sootMethod, exceptionInfo);

        if(exceptionInfo.getRelatedParamValues().size()>0 && exceptionInfo.getRelatedFieldValues().size() ==0) {
            List<String> newTrace = new ArrayList<>(trace);
            RelatedMethod addMethod = new RelatedMethod(sootMethod.getSignature(), RelatedMethodSource.CALLER, 0, newTrace);
            exceptionInfo.addRelatedMethodsInSameClassMap(addMethod);
            exceptionInfo.addRelatedMethods(sootMethod.getSignature());
            List<String> newTrace2 = new ArrayList<>(trace);
            getExceptionCallerByParam(sootMethod, exceptionInfo, new HashSet<>(), 1,
                    RelatedMethodSource.CALLER, exceptionInfo.getRelatedValueIndex(), newTrace2);
        }else if(exceptionInfo.getRelatedParamValues().size()==0 && exceptionInfo.getRelatedFieldValues().size()>0) {
            List<String> newTrace = new ArrayList<>(trace);
            getExceptionCallerByField(sootMethod, exceptionInfo, new HashSet<>(), 1,
                    RelatedMethodSource.FIELD, newTrace);
        }else if(exceptionInfo.getRelatedParamValues().size()>0 && exceptionInfo.getRelatedFieldValues().size()>0){
            List<String> newTrace = new ArrayList<>(trace);
            getExceptionCallerByField(sootMethod, exceptionInfo, new HashSet<>(), 1,
                    RelatedMethodSource.FIELD, newTrace);
            List<String> newTrace2 = new ArrayList<>(trace);
            getExceptionCallerByParam(sootMethod, exceptionInfo, new HashSet<>(),1,
                    RelatedMethodSource.CALLER, exceptionInfo.getRelatedValueIndex(), newTrace2);
        }
    }



    /**
     * getExceptionCaller
     * @param sootMethod
     * @param exceptionInfo
     * @param trace
     */
    private void getExceptionCallerByParam(SootMethod sootMethod, ExceptionInfo exceptionInfo,
                                           Set<SootMethod> callerHistory, int depth,
                                           RelatedMethodSource mtdSource, Set<Integer> paramIndexCallee, List<String> trace) {
        if(callerHistory.contains(sootMethod) || depth >ConstantUtils.CALLDEPTH)  return;
        callerHistory.add(sootMethod);
        Set<String> history = new HashSet<>();
        for (Iterator<Edge> it = Global.v().getAppModel().getCg().edgesInto(sootMethod); it.hasNext(); ) {
            Edge edge = it.next();
            SootMethod edgeSourceMtd = edge.getSrc().method();
            if(history.contains(edgeSourceMtd.getSignature())){
                continue;
            }
            history.add(edgeSourceMtd.getSignature());
            Set<Integer> paramIndexCaller = new HashSet<>();
            if(mtdSource == RelatedMethodSource.CALLER){
                paramIndexCaller = SootUtils.getIndexesFromMethod(edge, paramIndexCallee);
                if(paramIndexCaller.size() ==0 ) continue;
            }
            boolean flag = false;
            Set<SootClass> targetClasses = new HashSet<>();
            targetClasses.add(edgeSourceMtd.getDeclaringClass());
            List<SootClass> supers = SootUtils.getSuperClassesWithAbstract(edgeSourceMtd);
            targetClasses.addAll(supers);//android.app.ContextImpl && android.app.Context
            for (SootClass sootClass : targetClasses) {
                String signature = edgeSourceMtd.getSignature().replace(edgeSourceMtd.getDeclaringClass().getName(), sootClass.getName());
                SootMethod sm = SootUtils.getSootMethodBySignature(signature);
                String pkg1 = sootClass.getPackageName();
                String pkg2 = exceptionInfo.getSootMethod().getDeclaringClass().getPackageName();
                if (StringUtils.getPkgPrefix(pkg1, 2).equals(StringUtils.getPkgPrefix(pkg2, 2))
                        || edgeSourceMtd.getName().equals(sootMethod.getName())) {
                    if (edgeSourceMtd.getDeclaringClass() == sootClass) {
                        addRelatedMethodWithInfo(trace, signature, mtdSource, depth, edgeSourceMtd, exceptionInfo);
                        flag = true;
                    }
                    else if (supers.contains(sootClass)) {
                        addRelatedMethodWithInfo(trace, signature, mtdSource, depth, edgeSourceMtd, exceptionInfo);
                        flag = true;
                    }
                    if(sm!=null) {
                        Iterator<SootClass> it2 = sm.getDeclaringClass().getInterfaces().iterator();
                        while (it2.hasNext()) {
                            SootClass interfaceSC = it2.next();
                            for (SootMethod interfaceSM : interfaceSC.getMethods()) {
                                if(interfaceSM.getName().equals(sm.getName())) {
                                    addRelatedMethodWithInfo(trace, interfaceSM.getSignature(), mtdSource, depth, edgeSourceMtd, exceptionInfo);
                                    flag = true;
                                }
                            }
                        }
                    }
                }
            }
            if(flag) {
                List<String> newTrace = new ArrayList<>(trace);
                newTrace.add(0, edgeSourceMtd.getSignature());
                getExceptionCallerByParam(edgeSourceMtd, exceptionInfo, callerHistory, depth + 1, mtdSource, paramIndexCaller, newTrace);
            }
        }
    }

    private void addRelatedMethodWithInfo( List<String> trace, String signature, RelatedMethodSource mtdSource,
                                           int depth,  SootMethod edgeSourceMtd, ExceptionInfo exceptionInfo) {
        List<String> newTrace = new ArrayList<>(trace);
        newTrace.add(0, signature);
        RelatedMethod addMethodObj = new RelatedMethod(signature, mtdSource, depth, newTrace);
        addRelatedMethodInstance(edgeSourceMtd, addMethodObj, exceptionInfo);
    }

    private void addRelatedMethodInstance(SootMethod edgeSource, RelatedMethod addMethod, ExceptionInfo exceptionInfo) {
        if(edgeSource.isPublic()) {
            if (edgeSource.getDeclaringClass() == exceptionInfo.getSootMethod().getDeclaringClass())
                exceptionInfo.addRelatedMethodsInSameClassMap(addMethod);
            else
                exceptionInfo.addRelatedMethodsInDiffClassMap(addMethod);
            exceptionInfo.addRelatedMethods(addMethod.getMethod());
        }
    }


    /**
     * getExceptionCallerByField
     * @param sootMethod
     * @param exceptionInfo
     * @param callerHistory
     * @param depth
     * @param mtdSource
     * @param trace
     */
    private void getExceptionCallerByField(SootMethod sootMethod, ExceptionInfo exceptionInfo, HashSet<SootMethod> callerHistory, int depth, RelatedMethodSource mtdSource, List<String> trace) {
        for(SootField field: exceptionInfo.getRelatedFieldValues()){
            for(SootMethod otherMethod: sootMethod.getDeclaringClass().getMethods()){
                if(!otherMethod.hasActiveBody()) continue;
//                //if only one field and one condition, judge whether the condition is satisfied
//                //satisfy and not satisfy both may relate to bug fix... so do not judge
//                if(exceptionInfo.getRelatedFieldValues().size()==1 && exceptionInfo.getConditions().size()==1){
//                    boolean check = conditionCheck(exceptionInfo,otherMethod, field);
//                    if(!check) continue;
//                }
                if(SootUtils.fieldIsChanged(field, otherMethod)){
                    if(otherMethod.isPublic()) {
                        List<String> newTrace = new ArrayList<>(trace);
                        newTrace.add(0,"key field: " + field.toString());
                        newTrace.add(0,otherMethod.getSignature());
                        RelatedMethod addMethod = new RelatedMethod(otherMethod.getSignature(),mtdSource,depth, trace);
                        if(otherMethod.getDeclaringClass() == exceptionInfo.getSootMethod().getDeclaringClass()) {
                            exceptionInfo.addRelatedMethodsInSameClassMap(addMethod);
                        }
                        else {
                            exceptionInfo.addRelatedMethodsInDiffClassMap(addMethod);
                        }
                        exceptionInfo.addRelatedMethods(otherMethod.getSignature());
                    }
                    List<String> newTrace = new ArrayList<>(trace);
                    newTrace.add(0,"key field: " + field.toString());
                    newTrace.add(0,otherMethod.getSignature());
                    getExceptionCallerByParam(otherMethod, exceptionInfo, callerHistory,
                            depth+1, RelatedMethodSource.FIELDCALLER, new HashSet<>(), newTrace);
                }
            }
        }
    }

    /**
     * get the latest condition info for an ExceptionInfo
     * only analyze one level if condition, forward
     */
    private void getExceptionCondition(SootMethod sootMethod, Unit unit, ExceptionInfo exceptionInfo,
                                       Set<Unit> getCondHistory, boolean fromThrow, Unit lastGoto) {
        ConditionTracker conditionTracker = ConditionTracker.All;
        if(getCondHistory.contains(unit) || getCondHistory.size()> ConstantUtils.CONDITIONHISTORYSIZE) return;// if defUnit is not a pred of unit
        getCondHistory.add(unit);
        Body body = sootMethod.getActiveBody();
        ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(body);
        List<Unit> allPredsOfCurrentUnit = new ArrayList<>();
        SootUtils.getAllPredsofUnit(sootMethod, unit,allPredsOfCurrentUnit);
        List<Unit> allPredsOfThrowUnit = new ArrayList<>();
        SootUtils.getAllPredsofUnit(sootMethod, exceptionInfo.getUnit(),allPredsOfThrowUnit);
        List<Unit> gotoTargets = getGotoTargets(body);
        List<Unit> predsOf = unitGraph.getPredsOf(unit);
        for (Unit predUnit : predsOf) {
            if (predUnit instanceof IfStmt) {
                //direct condition or multiple condition
                if(conditionTracker == ConditionTracker.One){
                    if(exceptionInfo.getConditionUnits().size()>0) continue;
                } else if(conditionTracker == ConditionTracker.Three){
                    if(exceptionInfo.getConditionUnits().size()>=3) continue;
                }else if(conditionTracker == ConditionTracker.SmallBlock) {
                    // && ((IfStmt) predUnit).getTarget() != lastGoto
                    if (exceptionInfo.getConditionUnits().size() > 0 && lastGoto != null)
                        continue;
                }

                exceptionInfo.getTracedUnits().add(predUnit);
                IfStmt ifStmt = (IfStmt) predUnit;
                lastGoto = ifStmt.getTarget();
                //it is not a dominating condition
                if (allPredsOfThrowUnit.contains(ifStmt.getTarget()))
                    continue;
                Value cond = ifStmt.getCondition();
                exceptionInfo.addRelatedCondition(cond);
                exceptionInfo.getConditionUnits().add(ifStmt);

                if(cond instanceof ConditionExpr){
                    ConditionExpr conditionExpr = (ConditionExpr) cond;
                    ConditionWithValueSet conditionWithValueSet = new ConditionWithValueSet(sootMethod, ifStmt);
                    exceptionInfo.addRefinedConditions(conditionWithValueSet);
                    // add the direct condition

                    RefinedCondition rf = new RefinedCondition(conditionWithValueSet, conditionExpr.getOp1(),
                            SootUtils.getActualOp(conditionExpr), conditionExpr.getOp2(), predUnit);
                    conditionWithValueSet.addRefinedCondition(rf);

                    // trace from the direct condition
                    extendRelatedValues(conditionWithValueSet, sootMethod, allPredsOfCurrentUnit, exceptionInfo, predUnit, conditionExpr.getOp1(),
                            new ArrayList<>(),getCondHistory, fromThrow, "left");

                    extendRelatedValues(conditionWithValueSet, sootMethod, allPredsOfCurrentUnit, exceptionInfo, predUnit, conditionExpr.getOp2(),
                            new ArrayList<>(),getCondHistory, fromThrow, "right");

                }
            }else if (predUnit instanceof SwitchStmt) {
                exceptionInfo.getTracedUnits().add(predUnit);
                SwitchStmt swStmt = (SwitchStmt) predUnit;
                Value key = swStmt.getKey();
                exceptionInfo.addRelatedCondition(key);
                exceptionInfo.getConditionUnits().add(swStmt);
                //TODO
                ConditionWithValueSet conditionWithValueSet = new ConditionWithValueSet( sootMethod,predUnit);
                extendRelatedValues(conditionWithValueSet, sootMethod, allPredsOfCurrentUnit, exceptionInfo, predUnit, key, new ArrayList<>(), getCondHistory, fromThrow, "right");
            }else if (predUnit instanceof JIdentityStmt ) {
                JIdentityStmt stmt = (JIdentityStmt) predUnit;
                if(stmt.getRightOp() instanceof CaughtExceptionRef){
                    exceptionInfo.addCaughtedValues(stmt.getRightOp());
                    //analyzed try-catch contents
                }
            }
            if(conditionTracker == ConditionTracker.One){
                if(fromThrow  && exceptionInfo.getConditions().size()>0 ) continue;
            } else if(conditionTracker == ConditionTracker.Three){
                if(fromThrow  && exceptionInfo.getConditionUnits().size()>=3) continue;
            }else if(conditionTracker == ConditionTracker.SmallBlock) {
                if(fromThrow  && exceptionInfo.getConditions().size()>0 && gotoTargets.contains(predUnit))
                    continue;
            }
            getExceptionCondition(sootMethod, predUnit, exceptionInfo,getCondHistory, fromThrow, lastGoto);
        }
    }

    /**
     * tracing the values relates to the one used in if condition
     */
    private String extendRelatedValues(ConditionWithValueSet conditionWithValueSet, SootMethod sootMethod, List<Unit> allPreds, ExceptionInfo exceptionInfo, Unit unit, Value value,
                                     List<Value> valueHistory, Set<Unit> getCondHistory , boolean fromThrow, String location) {
        if(valueHistory.contains(value) || !allPreds.contains(unit)) return "";// if defUnit is not a pred of unit
        valueHistory.add(value);

        if(value instanceof  Local) {
            String methodSig = exceptionInfo.getSootMethod().getSignature();
            for(Unit defUnit: SootUtils.getDefOfLocal(methodSig,value, unit)) {
                if (defUnit instanceof JIdentityStmt) {
                    JIdentityStmt identityStmt = (JIdentityStmt) defUnit;
                    //add refinedCondition
                    addRefinedConditionIntoSet(conditionWithValueSet,value, "denote", identityStmt.getRightOp(), defUnit, location);
                    if (identityStmt.getRightOp() instanceof ParameterRef) {//from parameter
                        exceptionInfo.addRelatedParamValue(identityStmt.getRightOp());
                        int id = ((ParameterRef) identityStmt.getRightOp()).getIndex();
                        exceptionInfo.getRelatedValueIndex().add(id);
                        traceCallerOfParamValue(sootMethod, exceptionInfo, id, 1);
                        exceptionInfo.addCallerOfSingnlar2SourceVar(sootMethod.getSignature(), id);
                        return "ParameterRef";
                    }else if(identityStmt.getRightOp() instanceof CaughtExceptionRef){
                        exceptionInfo.addCaughtedValues(identityStmt.getRightOp());
                        return "CaughtExceptionRef";
                    }else if(identityStmt.getRightOp() instanceof ThisRef){
                        //add refinedCondition
                       return "ThisRef";
                    }
                } else if (defUnit instanceof JAssignStmt) {
                    JAssignStmt assignStmt = (JAssignStmt) defUnit;
                    //add refinedCondition
                    Value rightOp = assignStmt.getRightOp();
                    if (rightOp instanceof Local) {
                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo,
                                defUnit, rightOp, valueHistory, getCondHistory, fromThrow, location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, "equals", rightOp, assignStmt, location);
                    } else if (rightOp instanceof AbstractInstanceFieldRef) {
                        //if com.iscas.crashtracker.base is from parameter, field is omitted, if com.iscas.crashtracker.base is this, parameter is recorded
                        Value base = ((AbstractInstanceFieldRef) rightOp).getBase();
                        String defType = extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo,
                                defUnit, base, valueHistory, getCondHistory, fromThrow, location);
                        //if the this variable is assigned from parameter, it is not field related.
                        if(defType.equals("ThisRef")){
                            SootField field = ((AbstractInstanceFieldRef) rightOp).getField();
                            Value baseF = ((AbstractInstanceFieldRef) rightOp).getBase();
                            List<Value> rightValues = SootUtils.getFiledValueAssigns(baseF, field, allPreds);
                            for(Value rv: rightValues){
                                extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                        rv, valueHistory, getCondHistory, fromThrow, location);
                            }
                            exceptionInfo.addRelatedFieldValues(field);
                            addRefinedConditionIntoSet(conditionWithValueSet,value, "denote", rightOp, assignStmt, location);
                        }
                    } else if (rightOp instanceof Expr) {
                        if (rightOp instanceof InvokeExpr) {
                            InvokeExpr invokeExpr = SootUtils.getInvokeExp(defUnit);
                            for (Value val : invokeExpr.getArgs())
                                extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                        val, valueHistory, getCondHistory, fromThrow, location);
                            if (rightOp instanceof InstanceInvokeExpr) {
                                extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                        ((InstanceInvokeExpr) rightOp).getBase(),
                                        valueHistory, getCondHistory, fromThrow, location);
                                addRefinedConditionIntoSet(conditionWithValueSet,assignStmt.getLeftOp(), "is invoke", rightOp, assignStmt, location);
                            }else{
                                addRefinedConditionIntoSet(conditionWithValueSet,value, "equals", rightOp, assignStmt, location);
                            }
                        }else if(rightOp instanceof PhiExpr){
                            PhiExpr phiExpr = (PhiExpr) rightOp;
                            for(Value phiValue : phiExpr.getValues()) {
                                extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                        phiValue, valueHistory, getCondHistory, fromThrow, location);
                                addRefinedConditionIntoSet(conditionWithValueSet, value, "phi replace", phiValue, assignStmt, location);
                            }
                        } else {
                            if (rightOp instanceof AbstractInstanceOfExpr || rightOp instanceof AbstractCastExpr
                                    || rightOp instanceof AbstractBinopExpr || rightOp instanceof AbstractUnopExpr) {
                                for (ValueBox vb : rightOp.getUseBoxes()) {
                                    extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                            vb.getValue(), valueHistory, getCondHistory, fromThrow, location);
                                }
                            } else if (rightOp instanceof NewExpr) {
                                List<UnitValueBoxPair> usesOfOps = SootUtils.getUseOfLocal(exceptionInfo.getSootMethod().getSignature(), defUnit);
                                for (UnitValueBoxPair use : usesOfOps) {
                                    for (ValueBox vb : use.getUnit().getUseBoxes())
                                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, use.getUnit(), vb.getValue(),
                                                valueHistory, getCondHistory, fromThrow, location);
                                }
                            } else {
                                getExceptionCondition(exceptionInfo.getSootMethod(), defUnit, exceptionInfo, getCondHistory, fromThrow, null);
                            }
                            addRefinedConditionIntoSet(conditionWithValueSet,value, "equals", rightOp, assignStmt, location);
                        }
                    } else if (rightOp instanceof StaticFieldRef) {
                        //from static field value
                        exceptionInfo.addRelatedFieldValues(((StaticFieldRef) rightOp).getField());
                        addRefinedConditionIntoSet(conditionWithValueSet,value, "denote", rightOp, assignStmt, location);
                    }else if (rightOp instanceof JArrayRef) {
                        JArrayRef jArrayRef = (JArrayRef) rightOp;
                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                jArrayRef.getBase(), valueHistory, getCondHistory, fromThrow, location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, "equals", rightOp, assignStmt, location);
                    }else if (rightOp instanceof JInstanceFieldRef) {
                        JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defUnit,
                                jInstanceFieldRef.getBase(), valueHistory, getCondHistory, fromThrow, location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, "denote", rightOp, assignStmt, location);
                    }else {
                        getExceptionCondition(exceptionInfo.getSootMethod(), defUnit, exceptionInfo, getCondHistory, fromThrow, null);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, "equals", rightOp, assignStmt, location);
                    }
                } else {
                    log.info(defUnit.getClass().getName() + "::" + defUnit);
                }
            }
        }
        return "";
    }

    private void addRefinedConditionIntoSet(ConditionWithValueSet conditionWithValueSet, Value value, String operator, Value rightOp, Unit unit, String location) {
//        if(location.equals("right")) return;
        RefinedCondition refinedCondition = new RefinedCondition(conditionWithValueSet,value, operator, rightOp, unit);
        conditionWithValueSet.addRefinedCondition(refinedCondition);
    }

    private void traceCallerOfParamValue(SootMethod sootMethod,ExceptionInfo exceptionInfo, int id, int depth) {
        if(depth>ConstantUtils.SIGNLARCALLERDEPTH) return;
        //get the caller of sootMethod, and trace the usage of param
        for (Iterator<Edge> it = Global.v().getAppModel().getCg().edgesInto(sootMethod); it.hasNext(); ) {
            Edge edge = it.next();
            SootMethod edgeTgtMtd = edge.getTgt().method();
            SootMethod edgeSrcMtd = edge.getSrc().method();
            if(edgeTgtMtd == sootMethod){
                for(Unit u: SootUtils.getUnitListFromMethod(edgeSrcMtd)){
                    InvokeExpr invoke = SootUtils.getInvokeExp(u);
                    if(invoke!=null && invoke.getMethod().equals(edgeTgtMtd)){
                       if(invoke.getArgs().size()>id ){
                           //get the idth param
                           List<Unit> defs = SootUtils.getDefOfLocal(edgeSrcMtd.getSignature(), invoke.getArgs().get(id), u);
                           for(Unit def: defs){
                               if(def instanceof  IdentityStmt){
                                   if(((IdentityStmt)def).getRightOp() instanceof  ParameterRef) {
                                       int id2 = ((ParameterRef) ((IdentityStmt) def).getRightOp()).getIndex();
                                       exceptionInfo.addCallerOfSingnlar2SourceVar(edgeSrcMtd.getSignature(), id2);
                                       traceCallerOfParamValue(edgeSrcMtd, exceptionInfo, id2, depth + 1);
                                   }else if(((IdentityStmt)def).getRightOp() instanceof  ThisRef) {
                                       exceptionInfo.addCallerOfSingnlar2SourceVar(edgeSrcMtd.getSignature(), -1);
                                   }
                               }
                           }
                       }
                    }
                }
            }
        }
    }


    /**
     * get the goto destination of IfStatement
     */
    private List<Unit> getGotoTargets(Body body) {
        List<Unit> res = new ArrayList<>();
        for(Unit u : body.getUnits()){
            if(u instanceof JIfStmt){
                JIfStmt ifStmt = (JIfStmt)u;
                res.add(ifStmt.getTargetBox().getUnit());
            }
            else if(u instanceof GotoStmt){
                GotoStmt gotoStmt = (GotoStmt)u;
                res.add(gotoStmt.getTargetBox().getUnit());
            }
        }
        return res;
    }    /**
     * get the goto destination of IfStatement
     */
    private List<Unit> getGotoTargetsTwice(Body body) {
        List<Unit> temp = new ArrayList<>();
        List<Unit> res = new ArrayList<>();
        for(Unit u : body.getUnits()){
            if(u instanceof JIfStmt){
                JIfStmt ifStmt = (JIfStmt)u;
                if(temp.contains(ifStmt.getTargetBox().getUnit())){
                    res.add(ifStmt.getTargetBox().getUnit());
                }else {
                    temp.add(ifStmt.getTargetBox().getUnit());
                }
            }
            else if(u instanceof GotoStmt){
                GotoStmt gotoStmt = (GotoStmt)u;
                if(temp.contains(gotoStmt.getTargetBox().getUnit())){
                    res.add(gotoStmt.getTargetBox().getUnit());
                }else {
                    temp.add(gotoStmt.getTargetBox().getUnit());
                }
            }
        }
        return res;
    }

    /**
     * get the msg info for an ExceptionInfo
     */
    private void getExceptionMessage(SootMethod sootMethod, Unit unit, ExceptionInfo exceptionInfo, List<Integer> times){
        Body body = sootMethod.getActiveBody();
        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
        String exceptionClassName = exceptionInfo.getExceptionName();
        times.add(1);
        if (times.size() > 50) {
            return;
        }
        if(unit instanceof  ThrowStmt) {
            List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(), ((ThrowStmt) unit).getOp(), unit);
            Unit defUnit = defsOfOps.get(0);
            InvokeExpr invoke = SootUtils.getInvokeExp(defUnit);
            if(invoke!=null) {
                List<Unit> retList = SootUtils.getRetList(invoke.getMethod());
                for (Unit retU : retList) {
                    Value val = ((JReturnStmt) retU).getOp();
                    if (val instanceof Local) {
                        getExceptionMessage(invoke.getMethod(), retU, exceptionInfo, times);
                    }
                }
            }
        }
        List<Unit> predsOf = unitGraph.getPredsOf(unit);
        for (Unit predUnit : predsOf) {
            if (predUnit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) predUnit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                if (invokeExpr.getMethod().getDeclaringClass().toString().equals(exceptionClassName)) {
                    // 可能初始化会有多个参数，只关注第一个String参数
                    if (invokeExpr.getArgCount() > 0 && StringUtils.isStringType(invokeExpr.getArgs().get(0).getType())) {
                        Value arg = invokeExpr.getArgs().get(0);
                        if (arg instanceof Local) {
                            List<String> message = Lists.newArrayList();
                            message.add("");
                            getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) arg, unit, message);
                            String exceptionMsg = addQeSymbolToMessage(message.get(0));
                            exceptionInfo.setExceptionMsg(exceptionMsg);
                        } else if (arg instanceof Constant) {
                            StringConstant arg1 = (StringConstant) arg;
                            String exceptionMsg = addQeSymbolToMessage(arg1.value);
                            exceptionInfo.setExceptionMsg(exceptionMsg);
                        }
                    }
                } else {
                    getExceptionMessage(sootMethod, predUnit, exceptionInfo,times);
                }
            }
//            else {
//                getExceptionMessage(sootMethod, predUnit, exceptionInfo,times);
//            }
        }
    }

    private String addQeSymbolToMessage(String input) {
        String exceptionMsg = "";
        String[] ss =input.split("\\Q[\\s\\S]*\\E");
        for(int i= 0; i<ss.length-1;i++){
            exceptionMsg+="\\Q"+ss[i]+"\\E"+"[\\s\\S]*";
        }
        if(ss.length>=1)
            exceptionMsg+="\\Q"+ss[ss.length-1]+"\\E";
        if(input.endsWith("[\\s\\S]*"))
            exceptionMsg+="[\\s\\S]*";

        String temp = "";
        while(!exceptionMsg.equals(temp)) {
            temp= exceptionMsg;
            exceptionMsg = exceptionMsg.replace("\\Q\\E", "");
            exceptionMsg = exceptionMsg.replace("\\E\\Q", "");
            exceptionMsg = exceptionMsg.replace("[\\s\\S]*[\\s\\S]*", "[\\s\\S]*");
        }
        return exceptionMsg;
    }


    /**
     * getMsgContentByTracingValue
     */
    private void getMsgContentByTracingValue(ExceptionInfo exceptionInfo, SootMethod sootMethod, Local localTemp, Unit unit, List<String> message){
        List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),localTemp, unit);
        if(defsOfOps==null || defsOfOps.size()==0) return;
        Unit defOfLocal = defsOfOps.get(0);
        if (defOfLocal.equals(unit)) {
            return;
        }
        if (defOfLocal instanceof DefinitionStmt) {
            Value rightOp = ((DefinitionStmt) defOfLocal).getRightOp();
            if (rightOp instanceof Constant) {
                String s = message.get(0) + rightOp;
                message.set(0,s);
            } else if (rightOp instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                String invokeSig = invokeExpr.getMethod().getSignature();
                if (invokeSig.equals("<java.lang.StringBuilder: java.lang.String toString()>")) {
                    Value value = invokeExpr.getUseBoxes().get(0).getValue();
                    if (value instanceof Local) {
                        getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) value, unit, message);
                    }
                } else if (invokeSig.startsWith("<java.lang.StringBuilder: java.lang.StringBuilder append")) {
                    Value argConstant = invokeExpr.getArgs().get(0);
                    String s;
                    if (argConstant instanceof Constant) {
                        if (argConstant instanceof StringConstant) {
                            String value = ((StringConstant) argConstant).value;
                            s = value + message.get(0);
                        } else {
                            s = argConstant + message.get(0);
                        }

                    } else {
                        s = "[\\s\\S]*" + message.get(0) ;
                        //add this as related value
//                        List<Unit> allPreds = new ArrayList<>();
//                        SootUtils.getAllPredsofUnit(sootMethod, defOfLocal,allPreds);
//                        allPreds.add(defOfLocal);
//                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defOfLocal, argConstant, new ArrayList<>(), new HashSet<>(), true);
                    }
                    message.set(0, s);

                    Value value = ((JVirtualInvokeExpr) invokeExpr).getBaseBox().getValue();
                    if (value instanceof Local) {
                        getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) value, unit, message);
                    }
                }
            } else if (rightOp instanceof NewExpr) {
                NewExpr rightOp1 = (NewExpr) rightOp;
                if (rightOp1.getBaseType().toString().equals("java.lang.StringBuilder")) {
                    traceStringBuilderBack(exceptionInfo, sootMethod, defOfLocal, message, 0);
                }
            } else if (rightOp instanceof Local) {
                getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) rightOp, unit, message ) ;
            }
        }
    }

    /**
     * traceStringBuilderBack
     */
    private void traceStringBuilderBack(ExceptionInfo exceptionInfo, SootMethod sootMethod, Unit unit, List<String> message, int index){
        if (index > 10) {
            return;
        }
        Body body = sootMethod.getActiveBody();
        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
        List<Unit> succsOf = unitGraph.getSuccsOf(unit);
        for (Unit succs : succsOf) {
            if (succs instanceof InvokeStmt) {
                InvokeExpr invokeExpr = ((InvokeStmt) succs).getInvokeExpr();
                String invokeSig = invokeExpr.getMethod().getSignature();
                if (invokeSig.startsWith("<java.lang.StringBuilder: java.lang.StringBuilder append")) {
                    Value argConstant = invokeExpr.getArgs().get(0);
                    String s;
                    if (argConstant instanceof Constant) {
                        if (argConstant instanceof StringConstant) {
                            String value = ((StringConstant) argConstant).value;
                            s = message.get(0) + value;
                        } else {
                            s = message.get(0) + argConstant;
                        }
                    } else{
                        s = message.get(0) + "[\\s\\S]*";
                        //add this as related value
//                        List<Unit> allPreds = new ArrayList<>();
//                        SootUtils.getAllPredsofUnit(sootMethod, succs,allPreds);
//                        allPreds.add(succs);
//                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, succs, argConstant, new ArrayList<>(), new HashSet<>(), true);
                    }
                    message.set(0, s);
                }
            } else if (succs instanceof ThrowStmt) {
                return;
            }
            traceStringBuilderBack(exceptionInfo, sootMethod, succs, message, index + 1);
        }
    }


    private boolean containPermissionString(String msg) {
        if(msg==null) return false;
        for(String str: permissionSet){
            if(msg.contains(str.trim().replace("android.permission.",""))){
                return true;
            }
        }
        return  false;
    }
    private HashSet<SootClass> getApplicationClasses() {
        HashSet<SootClass> applicationClasses = new HashSet<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if(!sootClass.isConcrete()) continue;
//            if (!sootClass.getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX)) continue;
            applicationClasses.add(sootClass);
        }
        return  applicationClasses;
    }



}
