package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.model.analyzeModel.*;
import com.iscas.exceptionextractor.utils.CFGTraverse;
import com.iscas.exceptionextractor.utils.PDGUtils;
import com.iscas.exceptionextractor.utils.PrintUtils;
import com.iscas.exceptionextractor.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
        getThrownExceptionList();
        outputThrowExceptionInfo();
        log.info("getThrownExceptionList end...");
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
        for (SootMethod sootMethod : Global.v().getAppModel().getTopoMethodQueue()) {
            cnt++;
            if (cnt % 200 == 0)
                log.info(String.format("This is the method #%d/%d, avg time per method: %.2fms",
                        cnt, totalCnt, (totalTime / ((double) cnt))));
            long startMS = System.currentTimeMillis();

            if(openFilter && filterMethod(sootMethod)) continue;

            if (!sootMethod.hasActiveBody()) continue;
            if(isInterProcedure)
                getCalleeExceptionOfAll(sootMethod);
            extractExceptionInfoOfMethod(sootMethod);

            if (MyConfig.getInstance().isStopFlag()) return;
            totalTime += (System.currentTimeMillis() - startMS);
        }
    }


    private void getCalleeExceptionOfAll(SootMethod sootMethod) {
        List<ConditionWithValueSet> conditionWithValueSetList = new ArrayList<>();
        for(Unit unit : sootMethod.getActiveBody().getUnits()){
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(unit);
            //for the callee
            if(invokeExpr !=null){
                SootMethod callee = invokeExpr.getMethod();
                if(callee==sootMethod) continue;
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
                                    List<String> toBeAdd = new ArrayList<>();
                                    for (RefinedCondition refinedCondition : newOne.getRefinedConditions()) {
                                        if (refinedCondition.toString().contains("parameter" + idEntry.getKey())) {
                                            String newCondition = refinedCondition.toString().replace("parameter" + idEntry.getKey(), "parameter" + idEntry.getValue());
                                            toBeAdd.add(newCondition);
                                        }
                                    }
                                    for(String newCondition: toBeAdd){
                                        newOne.addRefinedCondition(new RefinedCondition(newCondition));
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

    /**
     * for all the paths that end at throw unit, extract exception information from it
     * @param sootMethod
     */
    private void extractExceptionInfoOfMethod(SootMethod sootMethod) {
        HashSet<String> historyPath = new HashSet<>();
        List<List<Unit>> allPaths = getThrowEndPaths(sootMethod);

        PDGUtils pdgUtils = new PDGUtils(sootMethod, new ExceptionalUnitGraph(sootMethod.getActiveBody()));
        pdgUtils.analyzeThrowControlDependency();

        //for each throw unit in the end of a path
        for(List<Unit> path: allPaths) {
            if(path.get(path.size()-1) instanceof  ThrowStmt) {
                ThrowStmt throwStmt = (ThrowStmt) path.get(path.size()-1);
                if (throwStmt.getOp() instanceof  Local){
                    //if controlPath is the same, only reserve one
                    HashSet controlUnits = pdgUtils.getCDSMap().get(throwStmt);
                    List<ControlDependOnUnit> controlPath = PDGUtils.getControlPathFromPDG(controlUnits, path);
                    if(!historyPath.contains(PrintUtils.printList(controlPath))) {
                        historyPath.add(PrintUtils.printList(controlPath));
                        extractExceptionInfoOfUnit(sootMethod, throwStmt, controlPath);
                    }
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

    /**
     * remove repeated paths when only if stmts are reserved
     * @param sootMethod
     * @return
     */
    private List<List<Unit>> getThrowEndPaths(SootMethod sootMethod) {
        CFGTraverse cfgTraverse = new CFGTraverse(sootMethod);
        cfgTraverse.traverseAllPathsEndWithThrow();
        return  cfgTraverse.getAllPaths();
    }


    private void outputThrowExceptionInfo() {
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException");
        exceptionListElement.clear();
        ExceptionInfoClientOutput.getSummaryJsonArrayOfThrownException2(thrownExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"thrownException2");
        ExceptionInfoClientOutput.printExceptionInfoList();
    }
}
