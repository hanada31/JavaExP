package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.client.soot.CFGTraverse;
import com.iscas.exceptionextractor.model.analyzeModel.ConditionTrackerInfo;
import com.iscas.exceptionextractor.model.analyzeModel.ExceptionInfo;
import com.iscas.exceptionextractor.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.shimple.PhiExpr;
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
    Set<List<DominateUnit>> dominatePathSet = new HashSet<>();
    public ExceptionAnalyzer() {
        super();
    }

    @Override
    public void analyze() {
        getDeclaredExceptionList();
        getThrownExceptionList();
        getCaughtExceptionList();
    }



    boolean openFilter = false;

    /**
     * for debugging only
     * @param sootMethod
     * @return
     */
    private boolean filterMethod(SootMethod sootMethod) {
        List<String> mtds = new ArrayList<>();
        mtds.add("throw_with_modified_value_condition2");
        for(String tag: mtds){
            if (sootMethod.getSignature().contains(tag)) {
                return false;
            }
        }
        return true;
    }



    /**
     * analyze the information of declared exceptions
     */
    private void getDeclaredExceptionList() {
        log.info("getDeclaredExceptionList start...");
        declaredExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
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
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
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
     * analyze the information of thrown exceptions
     */
    private void getThrownExceptionList() {
        log.info("getThrownExceptionList start...");
        thrownExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(openFilter && filterMethod(sootMethod)) continue;
                if (sootMethod.hasActiveBody()) {
                    List<ThrowStmt> throwList = new ArrayList<>();
                    for (Unit unit: sootMethod.getActiveBody().getUnits()) {
                        if (unit instanceof ThrowStmt) {
                            throwList.add((ThrowStmt) unit);
                        }
                    }
                    if(throwList.size()>0)
                        extractExceptionInfoOfAll(sootMethod, throwList);
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

    private void extractExceptionInfoOfAll(SootMethod sootMethod, List<ThrowStmt> throwList) {
        List<List<Unit>> allPaths = getThrowEndPaths(sootMethod);
//        removeRepeat(allPaths);
        for (ThrowStmt throwStmt: throwList) {
            Value throwValue = throwStmt.getOp();
            if (! (throwValue instanceof  Local)){
                System.err.println(throwStmt);
                continue;
            }else{
                SootClass exceptionClass = getThrowUnitWithType( sootMethod, (Unit) throwStmt, (Local) throwValue);
                for(List<Unit> path: allPaths) {
                    if(path.contains(throwStmt))
                        extractExceptionInfoFromUnit(sootMethod, throwStmt, exceptionClass, path);
                }
            }
        }
    }


    /**
     * remove repeated paths when only if stmts are reserved
     * @param sootMethod
     * @return
     */
    private List<List<Unit>> getThrowEndPaths(SootMethod sootMethod) {
        CFGTraverse cfgTraverse = new CFGTraverse(sootMethod);
        cfgTraverse.traverseAllPaths();
        List<List<Unit>> allPaths = cfgTraverse.getAllPaths();
        List<List<Unit>> newAllPaths = new ArrayList<>();
        for(List<Unit> path : allPaths){
           if (path.get(path.size()-1) instanceof  ThrowStmt)
               newAllPaths.add(path);
        }
        return  newAllPaths;
    }


    private void removeRepeat(List<List<Unit>> allPaths) {
        Set<List<Unit>> toBeDel = new HashSet<>();
        for(int i=0; i< allPaths.size(); i++){
            for(int j=i+1; j< allPaths.size(); j++){
                List<Unit> pathi  = allPaths.get(i);
                List<Unit> pathj  = allPaths.get(j);
                if(pathi.containsAll(pathj))
                    toBeDel.add(pathj);
                else if(pathj.containsAll(pathi)){
                    toBeDel.add(pathi);
                }
            }
        }
        allPaths.removeAll(toBeDel);

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
            }
        }
        return  method2unit2Value;
    }


    private void addThrowPoint(Map<SootMethod, Map<Unit, Local>> method2unit2Value, SootMethod sootMethod, Unit unit, Value throwValue) {
        if (!method2unit2Value.containsKey(sootMethod))
            method2unit2Value.put(sootMethod, new HashMap<>());
        method2unit2Value.get(sootMethod).put(unit, (Local) throwValue);
    }

    /**
     * get throw units with message from a method
     */
    public SootClass getThrowUnitWithType(SootMethod sootMethod, Unit unit, Local localTemp){
        List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),localTemp, unit);
        if (defsOfOps.size() == 0) return null;
        Unit defOfLocal = defsOfOps.get(0);
        if (defOfLocal.equals(unit)) return null;

        if (defOfLocal instanceof DefinitionStmt) {
            Value rightValue = ((DefinitionStmt)defOfLocal).getRightOp();
            if (rightValue instanceof NewExpr) {
                NewExpr newRightValue = (NewExpr) rightValue;
                return newRightValue.getBaseType().getSootClass();
            } else if (rightValue instanceof NewArrayExpr) {
                NewArrayExpr rightValue1 = (NewArrayExpr) rightValue;
                String s = rightValue1.getBaseType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    return Scene.v().getSootClass(s);
                }
            } else if (rightValue instanceof Local) {
                return getThrowUnitWithType(sootMethod, unit, (Local) rightValue);
            } else if (rightValue instanceof JCastExpr) {
                JCastExpr castExpr = (JCastExpr) rightValue;
                String s = castExpr.getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    return Scene.v().getSootClass(s);
                } else {
                    Value value = castExpr.getOpBox().getValue();
                    if (value instanceof Local) {
                        return getThrowUnitWithType( sootMethod, unit, (Local) value);
                    }
                }
            } else if (rightValue instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) rightValue;
                Type returnType = invokeExpr.getMethod().getReturnType();
                if (returnType.toString().endsWith("Exception") || returnType.toString().equals("java.lang.Throwable")) {
                    return Scene.v().getSootClass(returnType.toString());
                }

            } else if (rightValue instanceof CaughtExceptionRef) {
                //todo
                //caught an Exception here
                //$r1 := @caughtexception;
            } else if (rightValue instanceof PhiExpr) {
                PhiExpr phiExpr = (PhiExpr) rightValue;
                for (ValueUnitPair arg : phiExpr.getArgs()) {
                    if (arg.getValue() instanceof Local) {
                        return getThrowUnitWithType(sootMethod, unit, (Local) arg.getValue());
                    }
                }
            } if (rightValue instanceof FieldRef) {
                FieldRef rightValue1 = (FieldRef) rightValue;
                String s = rightValue1.getField().getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    return Scene.v().getSootClass(s);
                }
            } else if (rightValue instanceof ParameterRef) {
                ParameterRef rightValue1 = (ParameterRef) rightValue;
                String s = rightValue1.getType().toString();
                if (s.endsWith("Exception") || s.equals("java.lang.Throwable")) {
                    return Scene.v().getSootClass(s);
                }
            }  else if (rightValue instanceof ArrayRef) {
                ArrayRef rightValue1 = (ArrayRef) rightValue;
                Value value = rightValue1.getBaseBox().getValue();
                if (value instanceof Local) {
                    return getThrowUnitWithType(sootMethod, unit, (Local) value);
                }
            }
        }
        return null;
    }


    /**
     * create a New ExceptionInfo object and add content
     */
    private void extractExceptionInfoFromUnit(SootMethod sootMethod, ThrowStmt throwUnit, SootClass exceptionClass, List<Unit> path) {
        //if dominatePaths is the same, only reserve one
        List<DominateUnit> dominatePaths = ConditionAnalyzer.getAllDominate(sootMethod, throwUnit, path);
        if(dominatePathSet.contains(dominatePaths)) return;
        dominatePathSet.add(dominatePaths);

        if(exceptionClass.getName().equals("java.lang.Throwable")) return;
        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, throwUnit, exceptionClass.getName());
        thrownExceptionInfoList.add(exceptionInfo);

        //get type of exception
        exceptionInfo.findExceptionType(exceptionClass);

        // get message of exception
        getMsgOfThrowUnit(exceptionInfo);

        //get condition of exception
        getConditionOfUnit(exceptionInfo, path);

        //print condition information
        ExceptionInfoClientOutput.outputExceptionConditions(exceptionInfo);
    }

    /**
     * getMsgOfThrowUnit
     * @param exceptionInfo
     */
    private void getMsgOfThrowUnit(ExceptionInfo exceptionInfo) {
        RegularExpressionAnalyzer regularExpressionAnalyzer =
                new RegularExpressionAnalyzer(exceptionInfo.getSootMethod(),exceptionInfo.getUnit(), exceptionInfo.getExceptionName());
        regularExpressionAnalyzer.analyze();
        exceptionInfo.setExceptionMsg(regularExpressionAnalyzer.getExceptionMsg());
    }

    /**
     * getConditionOfUnit
     * @param exceptionInfo
     * @param path
     */
    private void getConditionOfUnit(ExceptionInfo exceptionInfo, List<Unit> path) {
        ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(exceptionInfo.getSootMethod(),exceptionInfo.getUnit(), path);
        conditionAnalyzer.analyze();
        ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
        exceptionInfo.setConditionTrackerInfo(conditionTrackerInfo);
    }





}
