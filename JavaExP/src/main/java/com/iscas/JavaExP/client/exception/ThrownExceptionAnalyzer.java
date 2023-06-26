package com.iscas.JavaExP.client.exception;

import com.iscas.JavaExP.base.Global;
import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.model.analyzeModel.*;
import com.iscas.JavaExP.utils.*;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/23 14:39
 * @Version 1.0
 */
@Slf4j
public class ThrownExceptionAnalyzer extends ExceptionAnalyzer {
    List<ExceptionInfo> thrownExceptionInfoList;
    public List<ExceptionInfo> getThrownExceptionInfoList() {
        return thrownExceptionInfoList;
    }

    @Override
    public void analyze() {
        log.info("getThrownExceptionList start...");
        getThrownExceptionNumber();
        getThrownExceptionList();
        outputThrowExceptionInfo();
        log.info("getThrownExceptionList end...");
    }

    private void getThrownExceptionNumber() {
        Map map = new HashMap();
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            int num = 0;
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(openFilter && filterMethod(sootMethod)) continue;
                if (sootMethod.hasActiveBody()) {
                    for(Unit unit : sootMethod.getActiveBody().getUnits()){
                        if(unit instanceof  ThrowStmt){
                            num++;
                        }
                    }
                }
                map.put(sootClass.getName(), num);
            }
        }
        Map<String, Integer> sortMap = MapSort.sortMapByValues(map);
        FileUtils.writeText2File(MyConfig.getInstance().getExceptionFilePath() +"throwNumber.txt", PrintUtils.printMap(sortMap), false);
    }

    /**
     * analyze the information of thrown exceptions
     */
    private void getThrownExceptionList() {
        thrownExceptionInfoList = new ArrayList<>();
        int totalCnt = Global.v().getAppModel().getTopoMethodQueue().size();
        log.info("There are totally {} methods in TopoMethodQueue", totalCnt);
        int cnt = 0;
        long totalTime = 0;
        long lastTotalTime = -1;
        for (SootMethod sootMethod : Global.v().getAppModel().getTopoMethodQueue()) {
            cnt++;
            if (cnt % 200 == 0) {
                log.info(String.format("This is the method #%d/%d, avg time per method: %.2fms",
                        cnt, totalCnt, (totalTime-lastTotalTime) / 200.0));
                lastTotalTime = totalTime;
            }
            long startMS = System.currentTimeMillis();

            if(openFilter && filterMethod(sootMethod)) continue;
            if (!sootMethod.hasActiveBody()) continue;
            Map<Unit, List<ExceptionInfo>> InvokeStmtSetToCalleeWithException = new HashMap();
            // if callee throw an exception
            if(isInterProcedure)
                InvokeStmtSetToCalleeWithException = getCalleeExceptionOfAll(sootMethod, startMS);
            //analyze the method itself and combine the callee analysis
            extractExceptionInfoOfMethod(sootMethod, InvokeStmtSetToCalleeWithException, startMS);

            if (MyConfig.getInstance().isStopFlag()) return;
            totalTime += (System.currentTimeMillis() - startMS);
        }
    }


    private Map<Unit, List<ExceptionInfo>> getCalleeExceptionOfAll(SootMethod sootMethod, long startMS) {
        Map<Unit, List<ExceptionInfo>> InvokeStmtSetToCalleeWithException = new HashMap();
        for(Unit unit : sootMethod.getActiveBody().getUnits()){
//            if(System.currentTimeMillis() - startMS > ConstantUtils.SINGLEMETHODTIME) {
//                log.info("Timeout while executing getCalleeExceptionOfAll @ " +sootMethod.getSignature() );
//                continue;
//            }
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(unit);
            //for the callee
            if(invokeExpr !=null){
                SootMethod callee = invokeExpr.getMethod();
                if(callee==sootMethod) continue;
                if(callee.isJavaLibraryMethod()) continue;
                List<ExceptionInfo>  exceptionInfoList = Global.v().getAppModel().getMethod2ExceptionList().get(callee.getSignature());
                if(exceptionInfoList==null) continue;
                List<ExceptionInfo>  exceptionInfoListOfCallee = new ArrayList<>();
                InvokeStmtSetToCalleeWithException.put(unit,exceptionInfoListOfCallee);
                for(ExceptionInfo exceptionInfo: exceptionInfoList){
                    if(exceptionInfo.getConditionTrackerInfo().getRefinedConditions().size()==0){
                        //no parameter analysis
                        ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfoCopy);
                        exceptionInfoListOfCallee.add(exceptionInfoCopy);
                    }else {
                        //parameter analysis
                        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(sootMethod,unit);
                        if(formalPara2ActualPara.size()==0) continue;
                        ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                        exceptionInfoCopy.setExceptionMsg(exceptionInfo.getExceptionMsg());
                        Map<Unit, ConditionWithValueSet> refinedConditionsOld = exceptionInfo.getConditionTrackerInfo().getRefinedConditions();
                        for (Map.Entry<Unit, ConditionWithValueSet> refinedConditionEntry : refinedConditionsOld.entrySet()) {
                            try {
                                ConditionWithValueSet newOne = refinedConditionEntry.getValue().clone();
                                List<RefinedCondition> toBeAdd = new ArrayList<>();
                                List<RefinedCondition> toBeDel = new ArrayList<>();
                                for (Map.Entry<Integer, Integer> idEntry : formalPara2ActualPara.entrySet()) {
                                    int calleeId = idEntry.getKey()-1;
                                    int callerId = idEntry.getValue()-1;
                                    for (RefinedCondition refinedCondition : newOne.getRefinedConditions()) {
                                        if (refinedCondition.toString().contains("parameter" + calleeId)) {
                                            String newConditionStr = refinedCondition.toString().replace("parameter" + calleeId, "parameter" + callerId);
                                            RefinedCondition  newCondition = new RefinedCondition(newConditionStr);
                                            newCondition.setSatisfied(refinedCondition.isSatisfied());
                                            toBeAdd.add(newCondition);
                                            if(callerId!=calleeId) // if it is same, only add once for repeat ones
                                                toBeDel.add(refinedCondition);
                                        }
                                    }
                                }
                                for(RefinedCondition newCondition: toBeAdd){
                                    newOne.addRefinedCondition(newCondition);
                                }
                                for(RefinedCondition oldCondition: toBeDel){
                                    newOne.getRefinedConditions().remove(oldCondition);
                                }
                                exceptionInfoCopy.getConditionTrackerInfo().addRefinedConditions(newOne);
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }
                        }
                        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfoCopy);
                        exceptionInfoListOfCallee.add(exceptionInfoCopy);
                    }
                }
            }
        }
        return InvokeStmtSetToCalleeWithException;
    }

    /**
     * for all the paths that end at throw unit, extract exception information from it
     * @param sootMethod
     * @param invokeStmtSetToCalleeWithException
     */
    private void extractExceptionInfoOfMethod(SootMethod sootMethod, Map<Unit, List<ExceptionInfo>> invokeStmtSetToCalleeWithException, long startMS) {
        HashSet<String> historyPath = new HashSet<>();
        List<List<Unit>> allPaths = getThrowAndInvokeEndPaths(sootMethod, invokeStmtSetToCalleeWithException.keySet());
        PDGUtils pdgUtils = new PDGUtils(sootMethod, new ExceptionalUnitGraph(sootMethod.getActiveBody()));
        pdgUtils.analyzeThrowAndInvokeControlDependency();

        //for each throw unit in the end of a path
        for(List<Unit> path: allPaths) {
            Unit lastUnit = path.get(path.size()-1);
            //if controlPath is the same, only reserve one
            HashSet controlUnits = pdgUtils.getCDSMap().get(lastUnit);
            List<ControlDependOnUnit> controlPath = PDGUtils.getControlPathFromPDG(controlUnits, path);
            if(!historyPath.contains(PrintUtils.printList(controlPath))) {
                historyPath.add(PrintUtils.printList(controlPath));
                if(lastUnit instanceof  ThrowStmt) {
                    extractExceptionInfoOfUnit(sootMethod, (ThrowStmt) lastUnit, controlPath);
                }
                else {
                    //get the control dependency of invoke stmt and combine the result with callee analysis
                    mergeExceptionInfoOfUnit(sootMethod, lastUnit, controlPath, invokeStmtSetToCalleeWithException.get(lastUnit));
                }
            }
        }
    }

    /**
     *
     * for one path that ends at throw unit, extract exception information from it
     * create a New ExceptionInfo object and add content
     */
    private void extractExceptionInfoOfUnit(SootMethod sootMethod, ThrowStmt throwUnit, List<ControlDependOnUnit> controlPath) {
        SootClass exceptionClass = getThrowUnitWithType(sootMethod, throwUnit, (Local) throwUnit.getOp());
        if(exceptionClass==null || exceptionClass.getName().equals("java.lang.Throwable")) return;

        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, throwUnit, exceptionClass.getName());
        thrownExceptionInfoList.add(exceptionInfo);
        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfo);

        //get type of exception
        exceptionInfo.findExceptionType(exceptionClass);

        // get message of exception
        getMsgOfThrowUnit(exceptionInfo);

        //get condition of exception
        getConditionOfUnit(exceptionInfo, controlPath);

    }
    /**
     * mergeExceptionInfoOfUnit
     * analyze the conditions of call sites that invokes callee with exceptions
     */
    private void mergeExceptionInfoOfUnit(SootMethod sootMethod, Unit unit, List<ControlDependOnUnit> controlPath, List<ExceptionInfo> exceptionInfoSet) {
        //get condition of exception
        for(ExceptionInfo exceptionInfo: exceptionInfoSet) {
//            getConditionOfUnit(exceptionInfo, controlPath);
            ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(sootMethod,unit, controlPath);
            conditionAnalyzer.analyze();
            ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
            exceptionInfo.getConditionTrackerInfo().getRefinedConditions().putAll(conditionTrackerInfo.getRefinedConditions());
        }
    }

    /**
     * getConditionOfUnit
     * @param exceptionInfo
     * @param controlPath
     */
    private void getConditionOfUnit(ExceptionInfo exceptionInfo, List<ControlDependOnUnit> controlPath) {
        ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(exceptionInfo.getSootMethod(),exceptionInfo.getUnit(), controlPath);
        conditionAnalyzer.analyze();
        ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
        exceptionInfo.setConditionTrackerInfo(conditionTrackerInfo);
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


    private List<List<Unit>> getThrowEndPaths(SootMethod sootMethod) {
        CFGTraverse cfgTraverse = new CFGTraverse(sootMethod);
        cfgTraverse.traverseAllPathsEndWithThrow();
        return  cfgTraverse.getAllPaths();
    }

    private List<List<Unit>> getThrowAndInvokeEndPaths(SootMethod sootMethod, Set invokeSet) {
        CFGTraverse cfgTraverse = new CFGTraverse(sootMethod);
        long startMS = System.currentTimeMillis();
        cfgTraverse.traverseAllPathsEndWithThrowAndInvoke(invokeSet, startMS);
        return  cfgTraverse.getAllPaths();
    }


    private void outputThrowExceptionInfo() {
//        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
//        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException(thrownExceptionInfoList, exceptionListElement);
//        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException");
//        exceptionListElement.clear();
//        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException2(thrownExceptionInfoList, exceptionListElement);
//        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException2");
        ExceptionInfoClientOutput.printExceptionInfoList();
    }
}
