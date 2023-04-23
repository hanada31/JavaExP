package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.client.soot.CFGTraverse;
import com.iscas.exceptionextractor.model.analyzeModel.*;
import com.iscas.exceptionextractor.utils.PrintUtils;
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
    Set<String> dominatePathSet;

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
        mtds.add("throw_with_combined_condition5");
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
        for (SootMethod sootMethod : Global.v().getAppModel().getTopoMethodQueue()) {
            if(openFilter && filterMethod(sootMethod)) continue;
            if (!sootMethod.hasActiveBody()) continue;
            getCalleeExceptionOfAll(sootMethod);
            extractExceptionInfoOfAll(sootMethod);
        }
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException");
        exceptionListElement.clear();
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException2(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException2");
        ExceptionInfoClientOutput.printExceptionInfoList();
        log.info("getThrownExceptionList end...");
    }



    /**
     * analyze the information of thrown exceptions
     */
//    private void getThrownExceptionList_every_class() {
//        log.info("getThrownExceptionList start...");
//        thrownExceptionInfoList = new ArrayList<>();
//        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
//            for (SootMethod sootMethod : sootClass.getMethods()) {
//                if(openFilter && filterMethod(sootMethod)) continue;
//                if (!sootMethod.hasActiveBody()) continue;
//                    getCalleeExceptionOfAll(sootMethod);
//                    extractExceptionInfoOfAll(sootMethod);
//            }
//            ExceptionInfoClientOutput.writeThrownExceptionInJsonForCurrentClass(sootClass, thrownExceptionInfoList);
//        }
//        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
//        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException(thrownExceptionInfoList, exceptionListElement);
//        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException");
//        exceptionListElement.clear();
//        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException2(thrownExceptionInfoList, exceptionListElement);
//        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException2");
//
//        log.info("getThrownExceptionList end...");
//    }


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

    /**
     * for all the paths that end at throw unit, extract exception information from it
     * @param sootMethod
     */
    private void extractExceptionInfoOfAll(SootMethod sootMethod) {
        dominatePathSet = new HashSet<>();
        List<List<Unit>> allPaths = getThrowEndPaths(sootMethod);
//        removeRepeat(allPaths);
        for(List<Unit> path: allPaths) {
            if(path.get(path.size()-1) instanceof  ThrowStmt) {
                ThrowStmt throwStmt = (ThrowStmt) path.get(path.size()-1);
                if (throwStmt.getOp() instanceof  Local){
                    SootClass exceptionClass = getThrowUnitWithType(sootMethod, throwStmt, (Local) throwStmt.getOp());
                    extractExceptionInfoFromUnit(sootMethod, throwStmt, exceptionClass, path);
                }
            }
        }
    }
    /**
     * for one path that ends at throw unit, extract exception information from it
     * create a New ExceptionInfo object and add content
     */
    private void extractExceptionInfoFromUnit(SootMethod sootMethod, ThrowStmt throwUnit, SootClass exceptionClass, List<Unit> path) {
        if(exceptionClass.getName().equals("java.lang.Throwable")) return;
        //wrong inti place!!!!! every exception!
        //if dominatePaths is the same, only reserve one
        List<DominateUnit> dominatePaths = ConditionAnalyzer.getAllDominate(sootMethod, throwUnit, path);
        if(dominatePathSet.contains(PrintUtils.printList(dominatePaths))) {
            return;
        }
        dominatePathSet.add(PrintUtils.printList(dominatePaths));

        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, throwUnit, exceptionClass.getName());
        thrownExceptionInfoList.add(exceptionInfo);
        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfo);

        //get type of exception
        exceptionInfo.findExceptionType(exceptionClass);

        // get message of exception
        getMsgOfThrowUnit(exceptionInfo);


        //get condition of exception
        getConditionOfUnit(exceptionInfo, path);


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


    private void getCalleeExceptionOfAll(SootMethod sootMethod) {
        List<ConditionWithValueSet> conditionWithValueSetList = new ArrayList<>();
        for(Unit unit : sootMethod.getActiveBody().getUnits()){
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(unit);
            //for the callee
            if(invokeExpr !=null){
                SootMethod callee = invokeExpr.getMethod();
                if(callee.isJavaLibraryMethod()) continue;
                List<ExceptionInfo>  exceptionInfoList = Global.v().getAppModel().getMethod2ExceptionList().get(callee.getSignature());
                if(exceptionInfoList==null) continue;

                for(ExceptionInfo exceptionInfo: exceptionInfoList){
                    if(exceptionInfo.getConditionTrackerInfo().getRelatedCondType() == RelatedCondType.Empty){
                        //no parameter analysis
                        ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfoCopy);
                    }else {
                        //parameter analysis
                        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(sootMethod,unit);
                        if(formalPara2ActualPara.size()==0) continue;
                        ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                        Map<Unit, ConditionWithValueSet> refinedConditionsOld = exceptionInfo.getConditionTrackerInfo().getRefinedConditions();
                        for (Map.Entry<Unit, ConditionWithValueSet> refinedConditionEntry : refinedConditionsOld.entrySet()) {
                            try {
                                ConditionWithValueSet newOne = refinedConditionEntry.getValue().clone();
                                for (Map.Entry idEntry : formalPara2ActualPara.entrySet()) {
                                    for (RefinedCondition refinedCondition : newOne.getRefinedConditions()) {
                                        if (refinedCondition.toString().contains("parameter" + idEntry.getKey())) {
                                            String newCondition = refinedCondition.toString().replace("parameter" + idEntry.getKey(), "parameter" + idEntry.getValue());
                                            newOne.addRefinedCondition(new RefinedCondition(newCondition));
                                        }
                                    }
                                }
                                exceptionInfoCopy.getConditionTrackerInfo().addRefinedConditions(newOne);
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }
                        }
                        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfoCopy);
                    }
                }
            }
        }
    }

}
