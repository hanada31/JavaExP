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
import soot.jimple.internal.JNeExpr;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ValueUnitPair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author hanada
 * @Date 2023/4/23 14:39
 * @Version 1.0
 */
@Slf4j
public class ThrownExceptionAnalyzer extends ExceptionAnalyzer {
//    List<ExceptionInfo> thrownExceptionInfoList;
//    public List<ExceptionInfo> getThrownExceptionInfoList() {
//        return thrownExceptionInfoList;
//    }
    Map<Unit, List<ExceptionInfo>> InvokeStmtSetToCalleeWithException = new HashMap();
    Set<String> analyzedHistory = new HashSet<>();
    int cnt = 0;
    long totalTime = 0;
    long lastTotalTime = -1;

    @Override
    public void analyze() {
        log.info("getThrownExceptionList start...");
//        getThrownExceptionNumber();
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

//        thrownExceptionInfoList = new ArrayList<>();
        int totalCnt = Global.v().getAppModel().getTopoMethodQueue().size();
        log.info("There are totally {} methods in TopoMethodQueue", totalCnt);

        for (SootMethod sootMethod : Global.v().getAppModel().getTopoMethodQueue()) {
            cnt++;
            analyzeSootMethod(sootMethod,totalCnt);
            if (MyConfig.getInstance().isStopFlag()) return;
        }
        if(!MyConfig.getInstance().isLightWightMode()) {
            for (SootMethod sootMethod : Global.v().getAppModel().getTopoMethodQueue()) {
                List<ExceptionInfo> exceptionInfoList = Global.v().getAppModel().getMethod2ExceptionList().get(sootMethod.getSignature());
                if (exceptionInfoList == null || exceptionInfoList.size() == 0) {
                    cnt++;
                    analyzeSootMethod(sootMethod, totalCnt);
                    if (MyConfig.getInstance().isStopFlag()) return;
                }
            }
        }
    }

    private void analyzeSootMethod(SootMethod sootMethod, int totalCnt) {
        analyzedHistory.add(sootMethod.getSignature());
        if (cnt % 200 == 0) {
            log.info(String.format("This is the method #%d/%d, avg time per method: %.2fms",
                    cnt, totalCnt, (totalTime-lastTotalTime) / 200.0));
            lastTotalTime = totalTime;
        }
        long startMS = System.currentTimeMillis();

        if(openFilter && filterMethod(sootMethod)) return;
        if (!sootMethod.hasActiveBody()) return;
        //analyze the method itself and combine the callee analysis
        extractExceptionInfoOfMethod(sootMethod);
        totalTime += (System.currentTimeMillis() - startMS);
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
            }
            if(MyConfig.getInstance().isWriteOutput())
            updateControlPathFromRequireNotNull(controlPath, path);

            if(!historyPath.contains(PrintUtils.printList(controlPath) +lastUnit.toString())) {
                historyPath.add(PrintUtils.printList(controlPath) +lastUnit.toString());
                if(lastUnit instanceof  ThrowStmt) {
                    Trap trap = getTrapBlockOfCaughtException(sootMethod, path);
                    extractExceptionInfoOfUnit(sootMethod, (ThrowStmt) lastUnit, controlPath, trap);
                }
            }
        }
        if(MyConfig.getInstance().isInterProcedure()){
            for(List<Unit> path: allPaths) {
                HashSet<String> historyPath = new HashSet<>();
                Unit lastUnit = path.get(path.size() - 1);
                //if controlPath is the same, only reserve one
                HashSet controlUnits = pdgUtils.getCDSMap().get(lastUnit);
                List<ControlDependOnUnit> controlPath = PDGUtils.getControlPathFromPDG(controlUnits, path);
                if (!historyPath.contains(PrintUtils.printList(controlPath) + lastUnit.toString())) {
                    historyPath.add(PrintUtils.printList(controlPath) + lastUnit.toString());
                    if (!(lastUnit instanceof ThrowStmt)) {
                        if (SootUtils.getInvokeExp(lastUnit).getMethod().getSignature().contains(ConstantUtils.REQUIRENOTNULL)) {
                            ExceptionInfo exceptionInfo = extractExceptionOfRequireNonNull(sootMethod, lastUnit);
                            int order = SootUtils.getThrowUnitListFromMethod(sootMethod).indexOf(lastUnit);
                            exceptionInfo.setThrowUnitOrder(order);
                            List<ExceptionInfo> exceptionInfoListOfCallee = new ArrayList<>();
                            exceptionInfoListOfCallee.add(exceptionInfo);
                            mergeExceptionInfoOfUnit(sootMethod, lastUnit, path, controlPath, exceptionInfoListOfCallee);
                        } else if (isCaughtInterProcedureException(sootMethod, path) == false) {
                            List<ExceptionInfo> calleeExceptions = getCalleeExceptionOfLastUnit(sootMethod, lastUnit);
                            mergeExceptionInfoOfUnit(sootMethod, lastUnit, path, controlPath, calleeExceptions);
                        }
                    }
                }
            }
        }
    }
    private void updateControlPathFromRequireNotNull(List<ControlDependOnUnit> controlPath, List<Unit> path) {
        for(Unit u : path){
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(u);
            if(invokeExpr!=null && u.toString().contains(ConstantUtils.REQUIRENOTNULL)){
                IfStmt ifStmt = new JIfStmt(new JEqExpr(invokeExpr.getArgs().get(0),NullConstant.v()), u);
                ControlDependOnUnit controlDependOnUnit = new ControlDependOnUnit(ifStmt,false);
                controlPath.add(0,controlDependOnUnit);
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
            Map<String, List<ExceptionInfo>> method2ExceptionList = Global.v().getAppModel().getMethod2ExceptionList();
            List<ExceptionInfo>  exceptionInfoList = method2ExceptionList.get(callee.getSignature());
            if(exceptionInfoList==null || exceptionInfoList.size()==0 ){
                return exceptionInfoListOfCallee;
            }
            InvokeStmtSetToCalleeWithException.put(unit,exceptionInfoListOfCallee);
            for(ExceptionInfo exceptionInfo: exceptionInfoList){
                if(MyConfig.getInstance().isLightWightMode()) {
                    if (exceptionInfo.getCallChain().split("->").length > ConstantUtils.CGCALLCHAINLIMIT) continue;
                    if (method2ExceptionList.containsKey(sootMethod.getSignature()) &&
                            method2ExceptionList.get(sootMethod.getSignature()).size() > ConstantUtils.EXCEPTIONINFOSIZE)
                        continue;
                }
                ExceptionInfo exceptionInfoCopy = new ExceptionInfo(sootMethod, unit, exceptionInfo.getExceptionName());
                updateExceptionThrowInfo(exceptionInfo, exceptionInfoCopy, callee,unit);
                updateExceptionCondition(exceptionInfo, exceptionInfoCopy,sootMethod,unit);
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
    /**
     * updateExceptionCondition_old,
     * update caller with callee's constraints
     * replace unmatched constraints in callee to caller
     * @param exceptionInfoCallee
     * @param exceptionInfoToBeUpdate
     * @param sootMethod
     * @param invokeUnit
     */
    private void updateExceptionCondition_Reverse(ExceptionInfo exceptionInfoCallee, ExceptionInfo exceptionInfoToBeUpdate, SootMethod sootMethod, Unit invokeUnit) {
        if (exceptionInfoCallee == exceptionInfoToBeUpdate) return;
        ConditionWithValueSet refinedConditionsOld = exceptionInfoCallee.getConditionTrackerInfo().getKeyConditionWithValueSet();
        if(refinedConditionsOld==null) return;
        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(sootMethod, invokeUnit);
        for(Value cond :exceptionInfoCallee.getConditionTrackerInfo().getConditions())
            exceptionInfoToBeUpdate.getConditionTrackerInfo().addRelatedCondition(cond);
        try {
            ConditionWithValueSet cloneConditionWithValueSet = refinedConditionsOld.clone();
            List<RefinedCondition> toBeAdd = new ArrayList<>();
            List<RefinedCondition> tobeDel = new ArrayList<>();
            for (RefinedCondition refinedCondition : cloneConditionWithValueSet.getRefinedConditions()) {
                Set<String> parameters = extractParameters(refinedCondition.toString());
                for (int id : formalPara2ActualPara.keySet())
                    parameters.remove(Integer.toString(id - 1));
                String newConditionStr = refinedCondition.toString();
                for (String calleeId : parameters) {
                    newConditionStr = newConditionStr.replace("parameter" + calleeId, "parameter" + "_" + calleeId + "_in_method_" +
                            SootUtils.getInvokeExp(invokeUnit).getMethod().getName());
                }
                for (Map.Entry<Integer, Integer> idEntry : formalPara2ActualPara.entrySet()) {
                    int calleeId = idEntry.getKey() - 1;
                    int callerId = idEntry.getValue() - 1;
                    if (refinedCondition.toString().contains("parameter" + calleeId)) {
                        newConditionStr = newConditionStr.replace("parameter" + calleeId, "parameter" + callerId);
                    }
                }
                if (!newConditionStr.equals(refinedCondition.toString())) {
                    RefinedCondition newCondition = new RefinedCondition(newConditionStr);
                    newCondition.setSatisfied(refinedCondition.isSatisfied());
                    newCondition.changeSatisfied();
                    toBeAdd.add(newCondition);
                    tobeDel.add(refinedCondition);
                }
                else {
                    refinedCondition.changeSatisfied();
                }
            }
            for (RefinedCondition oldCondition : tobeDel) {
                cloneConditionWithValueSet.getRefinedConditions().remove(oldCondition);
            }
            for (RefinedCondition newCondition : toBeAdd) {
                cloneConditionWithValueSet.addRefinedCondition(newCondition);
            }
            boolean isAddToEmpty = false;
            if (exceptionInfoToBeUpdate.getConditionTrackerInfo().getConditionWithValueSetMap().size() == 0) {
                isAddToEmpty = true;
            }
            if (isAddToEmpty)
                exceptionInfoToBeUpdate.getConditionTrackerInfo().addConditionWithValueSet(cloneConditionWithValueSet);
            else
                exceptionInfoToBeUpdate.getConditionTrackerInfo().addConditionWithValueSetNotLast(cloneConditionWithValueSet);
        }catch(CloneNotSupportedException e){
            e.printStackTrace();
        }

    }
    /**
     * updateExceptionCondition_old,
     * update caller with callee's constraints
     * replace unmatched constraints in callee to caller
     * @param exceptionInfoCallee
     * @param exceptionInfoToBeUpdate
     * @param sootMethod
     * @param invokeUnit
     */
    private void updateExceptionCondition(ExceptionInfo exceptionInfoCallee, ExceptionInfo exceptionInfoToBeUpdate, SootMethod sootMethod, Unit invokeUnit) {
        if (exceptionInfoCallee == exceptionInfoToBeUpdate) return;
        Map<Integer, Integer> formalPara2ActualPara = SootUtils.getFormalPara2ActualPara(sootMethod, invokeUnit);
        Map<Unit, ConditionWithValueSet> refinedConditionsOld = exceptionInfoCallee.getConditionTrackerInfo().getConditionWithValueSetMap();
        for(Value cond :exceptionInfoCallee.getConditionTrackerInfo().getConditions())
            exceptionInfoToBeUpdate.getConditionTrackerInfo().addRelatedCondition(cond);
        boolean isAddToEmpty = false;
        if (exceptionInfoToBeUpdate.getConditionTrackerInfo().getConditionWithValueSetMap().size() == 0) {
            isAddToEmpty = true;
        }
        //TODO, multiple condition need merge to condition and reverse
        for (Map.Entry<Unit, ConditionWithValueSet> refinedConditionEntry : refinedConditionsOld.entrySet()) {
            try {
                ConditionWithValueSet cloneConditionWithValueSet = refinedConditionEntry.getValue().clone();

                List<RefinedCondition> toBeAdd = new ArrayList<>();
                List<RefinedCondition> tobeDel = new ArrayList<>();
                for (RefinedCondition refinedCondition : cloneConditionWithValueSet.getRefinedConditions()) {
                    Set<String> parameters = extractParameters(refinedCondition.toString());
                    for (int id : formalPara2ActualPara.keySet())
                        parameters.remove(Integer.toString(id - 1));
                    String newConditionStr = refinedCondition.toString();
                    for (String calleeId : parameters) {
                        newConditionStr = newConditionStr.replace("parameter" + calleeId, "parameter" + "_" + calleeId + "_in_method_" +
                                SootUtils.getInvokeExp(invokeUnit).getMethod().getName());
                    }
                    for (Map.Entry<Integer, Integer> idEntry : formalPara2ActualPara.entrySet()) {
                        int calleeId = idEntry.getKey() - 1;
                        int callerId = idEntry.getValue() - 1;
                        if (refinedCondition.toString().contains("parameter" + calleeId)) {
                            newConditionStr = newConditionStr.replace("parameter" + calleeId, "parameter" + callerId);
                        }
                    }
                    if (!newConditionStr.equals(refinedCondition.toString())) {
                        RefinedCondition newCondition = new RefinedCondition(newConditionStr);
                        newCondition.setSatisfied(refinedCondition.isSatisfied());

                        toBeAdd.add(newCondition);
                        tobeDel.add(refinedCondition);
                    }
                }
                for (RefinedCondition oldCondition : tobeDel) {
                    cloneConditionWithValueSet.getRefinedConditions().remove(oldCondition);
                }
                for (RefinedCondition newCondition : toBeAdd) {
                    cloneConditionWithValueSet.addRefinedCondition(newCondition);
                }
                if (isAddToEmpty)
                    exceptionInfoToBeUpdate.getConditionTrackerInfo().addConditionWithValueSet(cloneConditionWithValueSet);
                else
                    exceptionInfoToBeUpdate.getConditionTrackerInfo().addConditionWithValueSetNotLast(cloneConditionWithValueSet);
            }catch(CloneNotSupportedException e){
                e.printStackTrace();
            }
        }
    }

    public static Set<String> extractParameters(String input) {
        Set<String> parameters = new HashSet<>();

        // 构建正则表达式模式，匹配 "parameter" 后面的数字
        String regex = "parameter(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // 使用正则表达式查找匹配的子字符串
        while (matcher.find()) {
            // 将匹配的子字符串添加到列表中
            parameters.add(matcher.group(1));
        }

        return parameters;
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
            if(caughtEntryUnits.keySet().contains(unit)){
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
//        thrownExceptionInfoList.add(exceptionInfo);
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
//        thrownExceptionInfoList.add(exceptionInfo);
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
        for( ConditionWithValueSet conditionWithValueSet: conditionTrackerInfo.getConditionWithValueSetMap().values())
            exceptionInfo.getConditionTrackerInfo().addConditionWithValueSet(conditionWithValueSet);

        Global.v().getAppModel().addMethod2ExceptionListForOne(sootMethod.getSignature(), exceptionInfo);
        return  exceptionInfo;
    }

    /**
     * mergeExceptionInfoOfUnit
     * analyze the conditions of call sites that invokes callee with exceptions
     */
    private void mergeExceptionInfoOfUnit(SootMethod sootMethod, Unit unit, List<Unit> path, List<ControlDependOnUnit> controlPath, List<ExceptionInfo> exceptionInfoSet) {
        for(ExceptionInfo exceptionInfo: exceptionInfoSet) {
            ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(sootMethod, unit, controlPath);
            conditionAnalyzer.analyze();
            ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
            for( ConditionWithValueSet conditionWithValueSet: conditionTrackerInfo.getConditionWithValueSetMap().values())
                exceptionInfo.getConditionTrackerInfo().addConditionWithValueSetNotLast(conditionWithValueSet);
        }
        for (ExceptionInfo exceptionInfo : exceptionInfoSet) {
            updateConditionWithPreMtdCond(sootMethod, unit, path, exceptionInfo);
        }
    }


    private void updateConditionWithPreMtdCond(SootMethod sootMethod, Unit unit, List<Unit> path, ExceptionInfo exceptionInfoToBeUpdate) {
        if(!MyConfig.getInstance().isInterProcedure()) return;
        for(Unit unitInPath: path){
            if(unitInPath == unit) break;
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(unitInPath);
            if(invokeExpr!=null) {
                SootMethod callee = invokeExpr.getMethod();
                if(sootMethod==callee) continue;
                if (unitInPath.toString().contains(ConstantUtils.REQUIRENOTNULL)) {
                    List<ControlDependOnUnit> controlPathTemp = new ArrayList<>();
                    IfStmt ifStmt = new JIfStmt(new JNeExpr(invokeExpr.getArgs().get(0),NullConstant.v()), unitInPath);
                    ControlDependOnUnit controlDependOnUnit = new ControlDependOnUnit(ifStmt,true);
                    controlPathTemp.add(controlDependOnUnit);
                    ConditionAnalyzer conditionAnalyzer = new ConditionAnalyzer(sootMethod,unitInPath, controlPathTemp);
                    conditionAnalyzer.analyze();
                    ConditionTrackerInfo conditionTrackerInfo = conditionAnalyzer.getConditionTrackerInfo();
                    for( ConditionWithValueSet conditionWithValueSet: conditionTrackerInfo.getConditionWithValueSetMap().values())
                        exceptionInfoToBeUpdate.getConditionTrackerInfo().addConditionWithValueSetNotLast(conditionWithValueSet);
                } else {
                    if (unitInPath == unit) break;
                    List<ExceptionInfo> exceptionInfoList = Global.v().getAppModel().getMethod2ExceptionList().get(callee.getSignature());
                    if (exceptionInfoList == null) continue;
                    for (ExceptionInfo exceptionInfoCallee : exceptionInfoList) {
                        updateExceptionCondition_Reverse(exceptionInfoCallee, exceptionInfoToBeUpdate, sootMethod, unitInPath);
                    }
                }
            }
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

        if(!MyConfig.getInstance().isInterProcedure()) return;
        List<Unit> preUnits = new ArrayList<>();
        SootUtils.getAllPredsofUnit(exceptionInfo.getSootMethod(), exceptionInfo.getUnit(),preUnits);
        updateConditionWithPreMtdCond(exceptionInfo.getSootMethod(), exceptionInfo.getUnit(), preUnits, exceptionInfo);
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
