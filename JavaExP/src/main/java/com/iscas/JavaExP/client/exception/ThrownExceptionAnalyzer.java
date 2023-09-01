package com.iscas.JavaExP.client.exception;

import com.google.common.collect.Lists;
import com.iscas.JavaExP.base.Global;
import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.client.soot.PDGUtils;
import com.iscas.JavaExP.model.analyzeModel.*;
import com.iscas.JavaExP.model.sootAnalysisModel.NestableObj;
import com.iscas.JavaExP.utils.*;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JIfStmt;
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
    Map<Unit, List<ExceptionInfo>> InvokeStmtSetToCalleeWithException = new HashMap();

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
        if(MyConfig.getInstance().isInterProcedure())
            log.info("!!!!!! isInterProcedure");
        else
            log.info("######## no InterProcedure");
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
            //analyze the method itself and combine the callee analysis
            extractExceptionInfoOfMethod(sootMethod);
            if (MyConfig.getInstance().isStopFlag()) return;
            totalTime += (System.currentTimeMillis() - startMS);
        }
    }


    /**
     * for all the paths that end at throw unit, extract exception information from it
     * @param sootMethod
     */
    private void extractExceptionInfoOfMethod(SootMethod sootMethod) {
        List<List<Unit>> allPaths;
        if(MyConfig.getInstance().isInterProcedure()) {
            allPaths = getThrowAndInvokeEndPaths(sootMethod);
        }else {
            allPaths = getThrowEndPaths(sootMethod);
        }
        PDGUtils pdgUtils = new PDGUtils(sootMethod, new ExceptionalUnitGraph(sootMethod.getActiveBody()));
        pdgUtils.analyzeThrowAndInvokeControlDependency();
        //for each throw unit in the end of a path

        for(List<Unit> path: allPaths) {
            HashSet<String> historyPath = new HashSet<>();
            Unit lastUnit = path.get(path.size()-1);
            //if controlPath is the same, only reserve one
            HashSet controlUnits = pdgUtils.getCDSMap().get(lastUnit);
            List<ControlDependOnUnit> controlPath;
            if(controlUnits!=null) {
                controlPath = PDGUtils.getControlPathFromPDG(controlUnits, path);
            }else{
                controlPath = PDGUtils.getControlPathFromPDG(new HashSet<>(path), path);
//                controlPath = ConditionAnalyzer.getAllDominate_self_analysis(sootMethod, lastUnit, path);
            }
            updateControlPathWithRequireNotNull(controlPath, path);

            if(!historyPath.contains(PrintUtils.printList(controlPath) +lastUnit.toString())) {
                historyPath.add(PrintUtils.printList(controlPath) +lastUnit.toString());
                if(lastUnit instanceof  ThrowStmt) {
                    Trap trap = getTrapBlockOfCaughtException(sootMethod, path);
                    extractExceptionInfoOfUnit(sootMethod, (ThrowStmt) lastUnit, controlPath, trap);
                }
            }
        }
        for(List<Unit> path: allPaths) {
            HashSet<String> historyPath = new HashSet<>();
            Unit lastUnit = path.get(path.size()-1);
            //if controlPath is the same, only reserve one
            HashSet controlUnits = pdgUtils.getCDSMap().get(lastUnit);
            List<ControlDependOnUnit> controlPath = PDGUtils.getControlPathFromPDG(controlUnits, path);
            if(!historyPath.contains(PrintUtils.printList(controlPath) +lastUnit.toString())) {
                historyPath.add(PrintUtils.printList(controlPath) +lastUnit.toString());
                if(! (lastUnit instanceof  ThrowStmt)) {
                    if (SootUtils.getInvokeExp(lastUnit).getMethod().getSignature().contains(ConstantUtils.REQUIRENOTNULL)) {
                        ExceptionInfo exceptionInfo = extractExceptionOfRequireNonNull(sootMethod, lastUnit);
                        List<ExceptionInfo>  exceptionInfoListOfCallee = new ArrayList<>();
                        exceptionInfoListOfCallee.add(exceptionInfo);
                        mergeExceptionInfoOfUnit(sootMethod, lastUnit, controlPath, exceptionInfoListOfCallee);
                        int order = SootUtils.getThrowUnitListFromMethod(sootMethod).indexOf(lastUnit);
                        exceptionInfo.setThrowUnitOrder(order);
                    }
                    else if(isCaughtInterProcedureException(sootMethod, path) == false) {
                        List<ExceptionInfo> calleeExceptions = getCalleeExceptionOfLastUnit(sootMethod, lastUnit);
                        mergeExceptionInfoOfUnit(sootMethod, lastUnit, controlPath, calleeExceptions);
                    }
                }
            }
        }
    }

    private void updateControlPathWithRequireNotNull(List<ControlDependOnUnit> controlPath, List<Unit> path) {
        for(Unit u : path){
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(u);
            if(invokeExpr!=null && u.toString().contains(ConstantUtils.REQUIRENOTNULL)){
                IfStmt ifStmt = new JIfStmt(new JEqExpr(invokeExpr.getArgs().get(0),NullConstant.v()), u);
                ControlDependOnUnit controlDependOnUnit = new ControlDependOnUnit(ifStmt,false);
                controlPath.add(controlDependOnUnit);
            }
        }
    }


    /**]
     * get the exceptions thrown in the callee method
     * @param sootMethod
     * @param unit
     * @return
     */
    private List<ExceptionInfo> getCalleeExceptionOfLastUnit(SootMethod sootMethod, Unit unit) {
        if(InvokeStmtSetToCalleeWithException.containsKey(unit)){
            return InvokeStmtSetToCalleeWithException.get(unit);
        }
        List<ExceptionInfo>  exceptionInfoListOfCallee = new ArrayList<>();
        InvokeExpr invokeExpr = SootUtils.getInvokeExp(unit);
        //for the callee
        if(invokeExpr !=null){
            SootMethod callee = invokeExpr.getMethod();
            if(callee==sootMethod) return exceptionInfoListOfCallee;
            if(callee.isJavaLibraryMethod()) return exceptionInfoListOfCallee;
            List<ExceptionInfo>  exceptionInfoList = Global.v().getAppModel().getMethod2ExceptionList().get(callee.getSignature());
            if(exceptionInfoList==null) return exceptionInfoListOfCallee;
            InvokeStmtSetToCalleeWithException.put(unit,exceptionInfoListOfCallee);
            for(ExceptionInfo exceptionInfo: exceptionInfoList){
                ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                updateExceptionThrowInfo(exceptionInfo, exceptionInfoCopy, callee,unit);
                updateExceptionCondition(exceptionInfo, exceptionInfoCopy);
                updateExceptionMessageByConstantTracking(exceptionInfo, exceptionInfoCopy);
                Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfoCopy);
                exceptionInfoListOfCallee.add(exceptionInfoCopy);
            }
        }
        return exceptionInfoListOfCallee;
    }

    private void updateExceptionThrowInfo(ExceptionInfo exceptionInfo, ExceptionInfo exceptionInfoCopy, SootMethod callee, Unit invokeUnit) {
        if(exceptionInfo.getIntraThrowUnit()==null){
            exceptionInfoCopy.setIntraThrowUnit(exceptionInfo.getUnit());
        }else{
            exceptionInfoCopy.setIntraThrowUnit(exceptionInfo.getIntraThrowUnit());
        }
        if(exceptionInfo.getIntraThrowUnitMethod()==null){
            exceptionInfoCopy.setIntraThrowUnitMethod(exceptionInfo.getSootMethod());
        }else{
            exceptionInfoCopy.setIntraThrowUnitMethod(exceptionInfo.getIntraThrowUnitMethod());
        }
        exceptionInfoCopy.setCallChain(invokeUnit+" -> "+exceptionInfo.getCallChain());
        exceptionInfoCopy.setInvokedMethod(callee);
        exceptionInfoCopy.setExceptionMsg(exceptionInfo.getExceptionMsg());
    }

    private void updateExceptionMessageByConstantTracking(ExceptionInfo exceptionInfo, ExceptionInfo exceptionInfoCopy) {
        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(exceptionInfoCopy.getSootMethod(),exceptionInfoCopy.getUnit());
        String message = exceptionInfoCopy.getExceptionMsg();
        if(message.contains(ConstantUtils.FORMALPARA)) {
            boolean isVariable = false;
            for (Map.Entry<Integer, Integer> idEntry : formalPara2ActualPara.entrySet()) {
                int calleeId = idEntry.getKey() - 1;
                int callerId = idEntry.getValue() - 1;
                if (message.contains(ConstantUtils.FORMALPARA + calleeId)) {
                    exceptionInfoCopy.setExceptionMsg(message.replace(ConstantUtils.FORMALPARA + calleeId, ConstantUtils.FORMALPARA + callerId));
                    isVariable = true;
                }
            }
            if(isVariable == false){
                String idStr = message.split(ConstantUtils.FORMALPARA)[1].split(" ")[0];
                int parameterId = Integer.parseInt(idStr);
                ValueObtainer valueObtainer = new ValueObtainer(exceptionInfoCopy.getInvokedMethod().getSignature());
                InvokeExpr invokeExpr = SootUtils.getInvokeExp(exceptionInfoCopy.getUnit());
                if(invokeExpr!=null) {
                    if(invokeExpr.getArgCount()>parameterId) {
                        NestableObj obj = valueObtainer.getValueofVar(invokeExpr.getArg(parameterId), exceptionInfoCopy.getUnit(), 0);
                        if (obj.getValues().size() > 0) {
                            exceptionInfoCopy.setExceptionMsg(message.replace(ConstantUtils.FORMALPARA + parameterId+" ", obj.getValues().get(0)));
                        }
                    }
                }
            }
        }
    }
    private void updateExceptionCondition(ExceptionInfo exceptionInfo, ExceptionInfo exceptionInfoCopy) {
        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(exceptionInfo.getSootMethod(),exceptionInfo.getUnit());
        Map<Unit, ConditionWithValueSet> refinedConditionsOld = exceptionInfo.getConditionTrackerInfo().getRefinedConditions();
        exceptionInfoCopy.getConditionTrackerInfo().getConditions().addAll(exceptionInfo.getConditionTrackerInfo().getConditions());

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
    }


    /**
     * for inter-procedure exception, judge whether it is caught in the caller
     * @param sootMethod
     * @param path
     * @return
     */
    private boolean isCaughtInterProcedureException(SootMethod sootMethod, List<Unit> path) {
        Map<Unit, Trap> caughtEntryUnits = new HashMap();
        for (Trap trap : sootMethod.getActiveBody().getTraps()) {
            caughtEntryUnits.put(trap.getHandlerUnit(),trap);
        }
        for(Trap trap: caughtEntryUnits.values()){
            if(path.contains(trap.getBeginUnit()) && !path.contains(trap.getEndUnit())){
                return true;
            }
        }
        return false;
    }

    /**
     * if the thrown exception is from try catch, get the try catch block
     * @param sootMethod
     * @param path
     * @return
     */
    private Trap getTrapBlockOfCaughtException(SootMethod sootMethod, List<Unit> path) {
        Map<Unit, Trap> caughtEntryUnits = new HashMap();
        for (Trap trap : sootMethod.getActiveBody().getTraps()) {
            caughtEntryUnits.put(trap.getHandlerUnit(),trap);
        }
        Trap trap = null;
        for(Unit unit: path){
            if(caughtEntryUnits.keySet().contains(unit) && path.contains(caughtEntryUnits.get(unit).getBeginUnit())){
                trap= caughtEntryUnits.get(unit);
                break; //re do
            }
        }
        return trap;
    }

    /**
     *
     * for one path that ends at throw unit, extract exception information from it
     * create a New ExceptionInfo object and add content
     */
    private void extractExceptionInfoOfUnit(SootMethod sootMethod, ThrowStmt throwUnit, List<ControlDependOnUnit> controlPath, Trap trap) {
//        log.info("extractExceptionInfoOfUnit" + sootMethod.getSignature());
        SootClass exceptionClass = getThrowUnitWithType(sootMethod, throwUnit, (Local) throwUnit.getOp());
        if(exceptionClass==null || exceptionClass.getName().equals("java.lang.Throwable")) return;

        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, throwUnit, exceptionClass.getName());
        int order = SootUtils.getThrowUnitListFromMethod(sootMethod).indexOf(throwUnit);
        exceptionInfo.setThrowUnitOrder(order);
        exceptionInfo.setCallChain(throwUnit.toString());

        if(trap != null) {
            exceptionInfo.setRethrow(true);
            exceptionInfo.setTrap(trap);
        }
        thrownExceptionInfoList.add(exceptionInfo);
        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfo);

        //get type of exception
        exceptionInfo.findExceptionType(exceptionClass);

        // get message of exception
        getMsgOfThrowUnit(exceptionInfo);

        //get condition of exception
        getConditionOfUnit(exceptionInfo, controlPath);

    }


    private ExceptionInfo extractExceptionOfRequireNonNull(SootMethod sootMethod, Unit nullCheckUnit) {
        SootClass exceptionClass = Scene.v().getSootClass("java.lang.NullPointerException");
        ExceptionInfo exceptionInfo =  new ExceptionInfo(sootMethod, nullCheckUnit, exceptionClass.getName());
        int order = SootUtils.getThrowUnitListFromMethod(sootMethod).indexOf(nullCheckUnit);
        exceptionInfo.setThrowUnitOrder(order);
        exceptionInfo.setCallChain(nullCheckUnit.toString());
        exceptionInfo.findExceptionType(exceptionClass);
        thrownExceptionInfoList.add(exceptionInfo);
        InvokeExpr invokeExpr = SootUtils.getInvokeExp(nullCheckUnit);
        if(invokeExpr.getArgs().size()>1){
            RegularExpressionAnalyzer regularExpressionAnalyzer = new RegularExpressionAnalyzer(sootMethod, nullCheckUnit, exceptionInfo.getExceptionName());
            if(invokeExpr.getArgs().get(1) instanceof  StringConstant){
                StringConstant constant = (StringConstant) invokeExpr.getArgs().get(1);
                String exceptionMsg = regularExpressionAnalyzer.addQeSymbolToMessage(constant.value+" is null");
                exceptionInfo.setExceptionMsg(exceptionMsg);
            }else if(invokeExpr.getArgs().get(1) instanceof Local){
                Local var = (Local) invokeExpr.getArgs().get(1);
                List<String> message = Lists.newArrayList();
                message.add("");
                regularExpressionAnalyzer.getMsgContentByTracingValue(sootMethod, var, nullCheckUnit, message);
                String exceptionMsg="[\\s\\S]* is null";
                if(regularExpressionAnalyzer.getExceptionMsg().contains("\\E"))
                    exceptionMsg = regularExpressionAnalyzer.getExceptionMsg().replace("\\E"," is null\\E");
                else{
                    if(regularExpressionAnalyzer.getExceptionMsg().equals("[\\s\\S]*")){
                        ValueObtainer valueObtainer = new ValueObtainer(sootMethod.getSignature());
                        NestableObj obj = valueObtainer.getValueofVar(var,nullCheckUnit,0 );
                        if (obj.getValues().size() > 0) {
                            exceptionMsg = regularExpressionAnalyzer.addQeSymbolToMessage(obj.getValues().get(0)+" is null");
                        }
                    }
                }
                exceptionInfo.setExceptionMsg(exceptionMsg);
            }
        }
        //get condition of requireNotNull
        List<ControlDependOnUnit> controlPath = new ArrayList<>();
        IfStmt ifStmt = new JIfStmt(new JEqExpr(invokeExpr.getArgs().get(0),NullConstant.v()), nullCheckUnit);
        ControlDependOnUnit controlDependOnUnit = new ControlDependOnUnit(ifStmt,true);
        controlPath.add(controlDependOnUnit);
        ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(sootMethod,nullCheckUnit, controlPath);
        conditionAnalyzer.analyze();
        ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
        exceptionInfo.getConditionTrackerInfo().getRefinedConditions().putAll(conditionTrackerInfo.getRefinedConditions());


        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfo);
        return  exceptionInfo;
    }

    /**
     * mergeExceptionInfoOfUnit
     * analyze the conditions of call sites that invokes callee with exceptions
     */
    private void mergeExceptionInfoOfUnit(SootMethod sootMethod, Unit unit, List<ControlDependOnUnit> controlPath, List<ExceptionInfo> exceptionInfoSet) {
//        log.info("mergeExceptionInfoOfUnit" + sootMethod.getSignature());
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

    private List<List<Unit>> getThrowAndInvokeEndPaths(SootMethod sootMethod) {
        CFGTraverse cfgTraverse = new CFGTraverse(sootMethod);
        long startMS = System.currentTimeMillis();
        cfgTraverse.traverseAllPathsEndWithThrowAndInvoke(startMS);
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
        ExceptionInfoClientOutput.printExceptionInfoListOfAll();

    }
}
