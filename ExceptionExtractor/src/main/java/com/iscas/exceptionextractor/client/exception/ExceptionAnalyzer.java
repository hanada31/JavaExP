package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.model.analyzeModel.ConditionTrackerInfo;
import com.iscas.exceptionextractor.model.analyzeModel.ExceptionInfo;
import com.iscas.exceptionextractor.utils.ConstantUtils;
import com.iscas.exceptionextractor.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JNewExpr;
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
    Set<String> nonThrowUnits = new HashSet<>();

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
        mtds.add("throw_with_outVar_condition(");
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
                    Map<SootMethod, Map<Unit, Local>> method2unit2Value = getThrowUnitWithValue(sootMethod,new  HashSet<>());
                    for(Map.Entry<SootMethod, Map<Unit,Local>> entryOuter: method2unit2Value.entrySet()) {
                        Map<Unit,Local> unit2Value = entryOuter.getValue();
                        Map<Unit, SootClass> unit2Type = new HashMap<>();
                        for (Map.Entry<Unit, Local> entryInner : unit2Value.entrySet()) {
                            getThrowUnitWithType(unit2Type, sootMethod, entryInner.getKey(), entryInner.getValue());
                        }
                        for (Map.Entry<Unit, SootClass> entryInner : unit2Type.entrySet()) {
                            extractExceptionInfoFromUnit(sootMethod, entryInner.getKey(), entryInner.getValue());
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
     * create a New ExceptionInfo object and add content
     */
    private void extractExceptionInfoFromUnit(SootMethod sootMethod, Unit throwUnit, SootClass exceptionName) {
        if(exceptionName.getName().equals("java.lang.Throwable")) return;
        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, throwUnit, exceptionName.getName());
        thrownExceptionInfoList.add(exceptionInfo);

        //get type of exception
        exceptionInfo.findExceptionType(exceptionName);

        // get message of exception
        getMsgOfThrowUnit(exceptionInfo);

        //get condition of exception
        getConditionOfUnit(exceptionInfo);

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
     */
    private void getConditionOfUnit( ExceptionInfo exceptionInfo) {
        ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(exceptionInfo.getSootMethod(),exceptionInfo.getUnit());
        conditionAnalyzer.analyze();
        ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
        exceptionInfo.setConditionTrackerInfo(conditionTrackerInfo);
    }





}
