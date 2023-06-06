package com.iscas.exceptionextractor.client.exception;

import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.model.analyzeModel.*;
import com.iscas.exceptionextractor.utils.*;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/18 9:51
 * @Version 1.0
 */
@Slf4j
public class ConditionAnalyzer  extends Analyzer {
    ConditionTrackerInfo conditionTrackerInfo;
    SootMethod mSootMethod;
    Unit mUnit;
    List<ControlDependOnUnit> controlDependOnPath;

    public ConditionTrackerInfo getConditionTrackerInfo() {
        return conditionTrackerInfo;
    }

    public ConditionAnalyzer(SootMethod sootMethod, Unit unit, List<ControlDependOnUnit> controlDependOnPath) {
        this.mSootMethod = sootMethod;
        this.mUnit = unit;
        this.controlDependOnPath = controlDependOnPath;
        conditionTrackerInfo = new ConditionTrackerInfo(sootMethod,unit);
    }

    @Override
    public void analyze() {
        getConditionAndValueFromUnit(mSootMethod, mUnit);
//        getReturnConditions();
//        try {
//            addOtherNotThrowConditions(mSootMethod, mUnit);
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * get RefinedCondition and related conditions
     * @param sootMethod
     * @param exceptionUnit
     */
    void getConditionAndValueFromUnit(SootMethod sootMethod, Unit exceptionUnit) {

        //get condition of unit
        getConditionFromUnit(sootMethod, exceptionUnit, new HashSet<>());

        //optimize condition of unit
        for(ConditionWithValueSet conditionWithValueSet :conditionTrackerInfo.getRefinedConditions().values() ) {
            conditionWithValueSet.optimizeCondition();
        }

        //get Related Methods
        getRelatedMethods(sootMethod);
    }

    /**
     * get Related Methods
     * @param sootMethod
     */
    private void getRelatedMethods(SootMethod sootMethod) {
        List<String> trace = new ArrayList<>();
        trace.add(sootMethod.getSignature());
        if(conditionTrackerInfo.getRelatedParamValues().size()>0 && conditionTrackerInfo.getRelatedFieldValues().size() ==0) {
            List<String> newTrace = new ArrayList<>(trace);
            RelatedMethod addMethod = new RelatedMethod(sootMethod.getSignature(), RelatedMethodSource.CALLER, 0, newTrace);
            conditionTrackerInfo.addRelatedMethodsInSameClassMap(addMethod);
            conditionTrackerInfo.addRelatedMethods(sootMethod.getSignature());
            List<String> newTrace2 = new ArrayList<>(trace);
            getExceptionCallerByParam(sootMethod, new HashSet<>(), 1,
                    RelatedMethodSource.CALLER, conditionTrackerInfo.getRelatedValueIndex(), newTrace2);
        }else if(conditionTrackerInfo.getRelatedParamValues().size()==0 && conditionTrackerInfo.getRelatedFieldValues().size()>0) {
            List<String> newTrace = new ArrayList<>(trace);
            getExceptionCallerByField(sootMethod, new HashSet<>(), 1,
                    RelatedMethodSource.FIELD, newTrace);
        }else if(conditionTrackerInfo.getRelatedParamValues().size()>0 && conditionTrackerInfo.getRelatedFieldValues().size()>0){
            List<String> newTrace = new ArrayList<>(trace);
            getExceptionCallerByField(sootMethod, new HashSet<>(), 1,
                    RelatedMethodSource.FIELD, newTrace);
            List<String> newTrace2 = new ArrayList<>(trace);
            getExceptionCallerByParam(sootMethod, new HashSet<>(),1,
                    RelatedMethodSource.CALLER, conditionTrackerInfo.getRelatedValueIndex(), newTrace2);
        }
    }

    private void addIntoMethod2ExceptionList(SootMethod sootMethod, Unit unit) throws CloneNotSupportedException {
        List<RefinedCondition> refinedConditionList = new ArrayList<>();
        Map<String, List<ExceptionInfo>> map = Global.v().getAppModel().getMethod2ExceptionList();
        for(ExceptionInfo exceptionInfo: map.get(sootMethod.getSignature())){
            if(exceptionInfo.getUnit() != unit){
                for(ConditionWithValueSet temp:exceptionInfo.getConditionTrackerInfo().getRefinedConditions().values())
                    refinedConditionList.addAll(temp.getRefinedConditions());
            }
        }
        for(RefinedCondition temp: refinedConditionList) {
            RefinedCondition newTemp = temp.clone();
            newTemp.changeSatisfied();
            ConditionWithValueSet conditionWithValueSet = new ConditionWithValueSet(sootMethod, newTemp.getUnit());
            conditionTrackerInfo.addRefinedConditions(conditionWithValueSet);
            conditionWithValueSet.addRefinedCondition(newTemp);
        }

    }

    /**
     * get the latest condition info for an conditionTrackerInfo
     * only analyze one level if condition, forward
     */
    void getConditionFromUnit(SootMethod sootMethod, Unit unit, Set<Unit> getCondHistory) {
        if(getCondHistory.contains(unit) || getCondHistory.size()> ConstantUtils.CONDITIONHISTORYSIZE) return;// if defUnit is not a pred of unit
        getCondHistory.add(unit);
        for (ControlDependOnUnit controlDependOnUnit : controlDependOnPath) {
            Unit predUnit = controlDependOnUnit.getUnit();
            boolean satisfied = controlDependOnUnit.isSatisfy();
            conditionTrackerInfo.getTracedUnits().add(predUnit);
            IfStmt ifStmt = (IfStmt) predUnit;

            Value cond = ifStmt.getCondition();
            conditionTrackerInfo.addRelatedCondition(cond);
            conditionTrackerInfo.getConditionUnits().add(ifStmt);

            if(cond instanceof ConditionExpr){
                ConditionExpr conditionExpr = (ConditionExpr) cond;
                ConditionWithValueSet conditionWithValueSet = new ConditionWithValueSet(sootMethod, ifStmt);
                conditionTrackerInfo.addRefinedConditions(conditionWithValueSet);
                // add the direct condition

                RefinedCondition rf = new RefinedCondition(conditionWithValueSet, conditionExpr.getOp1(),
                        SootUtils.getActualOp(conditionExpr), conditionExpr.getOp2(), predUnit, satisfied);
                conditionWithValueSet.addRefinedCondition(rf);


                // trace from the direct condition
                extendRelatedValues(conditionWithValueSet, sootMethod,  predUnit, conditionExpr.getOp1(),
                        new ArrayList<>(),getCondHistory,  "left");

                extendRelatedValues(conditionWithValueSet, sootMethod, predUnit,conditionExpr.getOp2(),
                        new ArrayList<>(),getCondHistory,  "right");
            }
        }
    }
//    List<ControlDependOnUnit> getAllDominateFromPDG(SootMethod sootMethod, Unit mUnit, List<Unit> mPath) {
//        List<ControlDependOnUnit> dominatePaths = new ArrayList<>();
//        HashSet<Unit> controlUnits = pdgUtils.getCDSMap().get(mUnit);
//        for(Unit controlUnit: controlUnits){
//            if(!(controlUnit instanceof  IfStmt)) continue;
//            IfStmt ifStmt = (IfStmt) controlUnit;
//            Unit ifTarget = ifStmt.getTarget();
//            Unit ifNext = mPath.get(mPath.indexOf(ifStmt)+1);
//            boolean satisfied = (ifTarget == ifNext)?true:false;
//            dominatePaths.add(new ControlDependOnUnit(ifStmt, satisfied));
//        }
//        //for shortcut paths
//        List<ControlDependOnUnit> dominatePathCopy = new ArrayList<>(dominatePaths);
//        for(ControlDependOnUnit controlDependOnUnit : dominatePathCopy){
//            if(!mPath.contains(controlDependOnUnit.unit))
//                dominatePaths.remove(controlDependOnUnit);
//        }
//        return dominatePaths;
//    }
//    static List<ControlDependOnUnit> getAllDominate_self_analysis(SootMethod sootMethod, Unit mUnit, List<Unit> mPath) {
//        List<Unit> allPredsOfThrowUnit = new ArrayList<>();
//        SootUtils.getAllPredsofUnit(sootMethod, mUnit,allPredsOfThrowUnit);
//        List<ControlDependOnUnit> dominatePaths = new ArrayList<>();
//        int size = -1;
//        while (size !=  dominatePaths.size()) {
//            size = dominatePaths.size();
//            for (Unit predUnit : sootMethod.getActiveBody().getUnits()) {
//                boolean newFind = true;
//                for(ControlDependOnUnit temp: dominatePaths){
//                    if(temp.unit == predUnit)  newFind = false;
//                }
//                if (predUnit instanceof IfStmt && newFind) {
//                    IfStmt ifStmt = (IfStmt) predUnit;
//                    //it is not a dominating condition
//                    boolean isDominate = getIsDominate(sootMethod, ifStmt, allPredsOfThrowUnit, dominatePaths);
//                    if (isDominate == true) {
//                        //judge the if statement is satisfied by where it goes to in mPath
//                        Unit ifTarget = ifStmt.getTarget();
//                        Unit ifNext = mPath.get(mPath.indexOf(ifStmt)+1);
//                        boolean satisfied = (ifTarget == ifNext)?true:false;
//                        dominatePaths.add(new ControlDependOnUnit(ifStmt, satisfied));
//                    }
//                }
//            }
//        }
//        //for shortcut paths
//        List<ControlDependOnUnit> dominatePathCopy = new ArrayList<>(dominatePaths);
//        for(ControlDependOnUnit controlDependOnUnit : dominatePathCopy){
//            if(!mPath.contains(controlDependOnUnit.unit))
//                dominatePaths.remove(controlDependOnUnit);
//        }
//        return dominatePaths;
//    }
//
//    static boolean getIsDominate(SootMethod sootMethod, IfStmt ifStmt, List<Unit> allPredsOfThrowUnit, List<ControlDependOnUnit> dominatePaths) {
//        if (!allPredsOfThrowUnit.contains(ifStmt.getTarget())) {
//            return true;
//        }
//        //data-flow analysis!!! whether an ifUnit can jump out throw unit before two branch merge.
//        //if the first common item in allpredsofthrowunit
//        List<Unit> list1 = new ArrayList<>();
//        list1.add(ifStmt.getTarget());
//        SootUtils.getAllSuccsofUnit(sootMethod, ifStmt.getTarget(),list1);
//        List<Unit> list2 = new ArrayList<>();
//        list2.add(SootUtils.getNotTargetOfIfUnit(sootMethod, ifStmt));
//        SootUtils.getAllSuccsofUnit(sootMethod, SootUtils.getNotTargetOfIfUnit(sootMethod, ifStmt),list2);
//        Unit common = CollectionUtils.getCommonElement(list1,list2);
//        if (common != null && !allPredsOfThrowUnit.contains(common)) {
//            return true;
//        }
//        if (common == null && !allPredsOfThrowUnit.contains(CollectionUtils.getLastItemInList(list1))) {
//            return true;
//        }
//        if (common == null && !allPredsOfThrowUnit.contains(CollectionUtils.getLastItemInList(list2))) {
//            return true;
//        }
//
//        // in same line with existing
//        for(ControlDependOnUnit dominateOne: dominatePaths){
//            if(dominateOne.unit.getJavaSourceStartLineNumber() == ifStmt.getJavaSourceStartLineNumber()) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * get not return check conditions
     */
    private void getReturnConditions() {
        int b = conditionTrackerInfo.getConditions().size();
        Set<Unit> retUnits = new HashSet<>();
        for (Unit u: mSootMethod.getActiveBody().getUnits()) {
            if (u instanceof ReturnStmt) {
                if(!isConditionOfRetIsCaughtException(mSootMethod, u, new HashSet<>()))
                    retUnits.add(u);
            }
        }
        for(Unit condUnit: conditionTrackerInfo.getConditionUnits()) {
            getRetUnitsFlowIntoConditionUnits(mSootMethod, condUnit, retUnits, new HashSet<>());
        }
        for (Unit retUnit : retUnits) {
            getConditionAndValueFromUnit(mSootMethod, retUnit);
        }

        int e = conditionTrackerInfo.getConditions().size();
        getConditionType( e-b);
    }


    /**
     * get RelatedCondType
     * @param retValue
     */
    void getConditionType(int retValue) {
        if(retValue>0)
            conditionTrackerInfo.setRelatedCondType(RelatedCondType.NotReturn);
        else if(conditionTrackerInfo.getConditions().size()>0)
            conditionTrackerInfo.setRelatedCondType(RelatedCondType.Basic);
        else
            conditionTrackerInfo.setRelatedCondType(RelatedCondType.Empty);
    }

    /**
     * get the latest condition info for an conditionTrackerInfo
     * only analyze one level if condition, forward
     */
    boolean isConditionOfRetIsCaughtException(SootMethod sootMethod, Unit unit, HashSet<Unit> units) {
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
            boolean flag = isConditionOfRetIsCaughtException(sootMethod, predUnit, units);
            if(flag)
                return true;
        }
        return false;
    }

    /**
     * getRetUnitsFlowIntoConditionUnits
     * @param sootMethod
     * @param unit
     * @param retUnits
     * @param history
     */
    void getRetUnitsFlowIntoConditionUnits(SootMethod sootMethod, Unit unit, Set<Unit> retUnits, HashSet<Unit> history) {
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
    }


    /**
     * tracing the values relates to the one used in if condition
     */
    private String extendRelatedValues(ConditionWithValueSet conditionWithValueSet, SootMethod sootMethod, Unit unit, Value value,
                                       List<Value> valueHistory, Set<Unit> getCondHistory, String location) {
        if(valueHistory.contains(value)) return "";// if defUnit is not a pred of unit
        valueHistory.add(value);
        if(value instanceof Local) {
            String methodSig = mSootMethod.getSignature();
            for(Unit defUnit: SootUtils.getDefOfLocal(methodSig,value, unit)) {
                if (defUnit instanceof JIdentityStmt) {
                    JIdentityStmt identityStmt = (JIdentityStmt) defUnit;
                    //add refinedCondition
                    addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.denoteOP, identityStmt.getRightOp(), defUnit, location);
                    if (identityStmt.getRightOp() instanceof ParameterRef) {//from parameter
                        conditionTrackerInfo.addRelatedParamValue(identityStmt.getRightOp());
                        int id = ((ParameterRef) identityStmt.getRightOp()).getIndex();
                        conditionTrackerInfo.getRelatedValueIndex().add(id);
                        traceCallerOfParamValue(sootMethod, id, 1);
                        conditionTrackerInfo.addCallerOfSingnlar2SourceVar(sootMethod.getSignature(), id);
                        return "ParameterRef";
                    }else if(identityStmt.getRightOp() instanceof CaughtExceptionRef){
                        conditionTrackerInfo.addCaughtedValues(identityStmt.getRightOp());
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
                        extendRelatedValues(conditionWithValueSet, sootMethod,
                                defUnit, rightOp, valueHistory, getCondHistory, location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, rightOp, assignStmt, location);
                    } else if (rightOp instanceof AbstractInstanceFieldRef) {
                        //if com.iscas.crashtracker.base is from parameter, field is omitted, if com.iscas.crashtracker.base is this, parameter is recorded
                        Value base = ((AbstractInstanceFieldRef) rightOp).getBase();
                        String defType = extendRelatedValues(conditionWithValueSet, sootMethod,
                                defUnit, base, valueHistory, getCondHistory, location);
                        //if the this variable is assigned from parameter, it is not field related.
                        if(defType.equals("ThisRef")){
                            SootField field = ((AbstractInstanceFieldRef) rightOp).getField();
                            Value baseF = ((AbstractInstanceFieldRef) rightOp).getBase();
                            List<Value> rightValues = SootUtils.getFiledValueAssigns(mSootMethod, mUnit,baseF, field);
                            for(Value rv: rightValues){
                                extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                        rv, valueHistory, getCondHistory, location);
                            }
                            conditionTrackerInfo.addRelatedFieldValues(field);
                            addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.denoteOP, rightOp, assignStmt, location);
                        }
                    } else if (rightOp instanceof Expr) {
                        if (rightOp instanceof InvokeExpr) {
                            InvokeExpr invokeExpr = SootUtils.getInvokeExp(defUnit);
                            for (Value val : invokeExpr.getArgs())
                                extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                        val, valueHistory, getCondHistory, location);
                            if (rightOp instanceof InstanceInvokeExpr) {
                                extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                        ((InstanceInvokeExpr) rightOp).getBase(),
                                        valueHistory, getCondHistory,  location);
                                addRefinedConditionIntoSet(conditionWithValueSet,assignStmt.getLeftOp(), IROperator.isiInvokeOP, rightOp, assignStmt, location);
                            }else{
                                addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, rightOp, assignStmt, location);
                            }
                        }else if(rightOp instanceof PhiExpr){
                            PhiExpr phiExpr = (PhiExpr) rightOp;
                            for(Value phiValue : phiExpr.getValues()) {
                                extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                        phiValue, valueHistory, getCondHistory,  location);
                                addRefinedConditionIntoSet(conditionWithValueSet, value, IROperator.phiReplaceOp, phiValue, assignStmt, location);
                            }
                        }else if(rightOp instanceof JCastExpr){
                            JCastExpr castExpr = (JCastExpr) rightOp;
                            extendRelatedValues(conditionWithValueSet, sootMethod,
                                    defUnit, rightOp, valueHistory, getCondHistory, location);
                            if(castExpr.getOp() instanceof  Constant)
                                addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.denoteOP, castExpr.getOp(), assignStmt, location);
                            else
                                addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, castExpr.getOp(), assignStmt, location);

                        } else {
                            if (rightOp instanceof AbstractInstanceOfExpr || rightOp instanceof AbstractCastExpr
                                    || rightOp instanceof AbstractBinopExpr || rightOp instanceof AbstractUnopExpr) {
                                for (ValueBox vb : rightOp.getUseBoxes()) {
                                    extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                            vb.getValue(), valueHistory, getCondHistory,  location);
                                }
                            } else if (rightOp instanceof NewExpr) {
                                List<UnitValueBoxPair> usesOfOps = SootUtils.getUseOfLocal(mSootMethod.getSignature(), defUnit);
                                for (UnitValueBoxPair use : usesOfOps) {
                                    for (ValueBox vb : use.getUnit().getUseBoxes())
                                        extendRelatedValues(conditionWithValueSet, sootMethod, use.getUnit(), vb.getValue(),
                                                valueHistory, getCondHistory, location);
                                }
                            } else {
                                getConditionFromUnit(mSootMethod, defUnit, getCondHistory);
                            }
                            addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, rightOp, assignStmt, location);
                        }
                    } else if (rightOp instanceof StaticFieldRef) {
                        //from static field value
                        conditionTrackerInfo.addRelatedFieldValues(((StaticFieldRef) rightOp).getField());
                        addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.denoteOP, rightOp, assignStmt, location);
                    }else if (rightOp instanceof JArrayRef) {
                        JArrayRef jArrayRef = (JArrayRef) rightOp;
                        extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                jArrayRef.getBase(), valueHistory, getCondHistory,  location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, rightOp, assignStmt, location);
                    }else if (rightOp instanceof JInstanceFieldRef) {
                        JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
                        extendRelatedValues(conditionWithValueSet, sootMethod, defUnit,
                                jInstanceFieldRef.getBase(), valueHistory, getCondHistory,  location);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.denoteOP, rightOp, assignStmt, location);
                    }else {
                        getConditionFromUnit(mSootMethod, defUnit, getCondHistory);
                        addRefinedConditionIntoSet(conditionWithValueSet,value, IROperator.equalsOp, rightOp, assignStmt, location);
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
        RefinedCondition refinedCondition = new RefinedCondition(conditionWithValueSet,value, operator, rightOp, unit, true);
        conditionWithValueSet.addRefinedCondition(refinedCondition);
    }

    private void traceCallerOfParamValue(SootMethod sootMethod, int id, int depth) {
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
                                        conditionTrackerInfo.addCallerOfSingnlar2SourceVar(edgeSrcMtd.getSignature(), id2);
                                        traceCallerOfParamValue(edgeSrcMtd, id2, depth + 1);
                                    }else if(((IdentityStmt)def).getRightOp() instanceof  ThisRef) {
                                        conditionTrackerInfo.addCallerOfSingnlar2SourceVar(edgeSrcMtd.getSignature(), -1);
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
     * getExceptionCaller
     * @param sootMethod
     * @param trace
     */
    private void getExceptionCallerByParam(SootMethod sootMethod, Set<SootMethod> callerHistory, int depth,
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
                String pkg2 = mSootMethod.getDeclaringClass().getPackageName();
                if (StringUtils.getPkgPrefix(pkg1, 2).equals(StringUtils.getPkgPrefix(pkg2, 2))
                        || edgeSourceMtd.getName().equals(sootMethod.getName())) {
                    if (edgeSourceMtd.getDeclaringClass() == sootClass) {
                        addRelatedMethodWithInfo(trace, signature, mtdSource, depth, edgeSourceMtd);
                        flag = true;
                    }
                    else if (supers.contains(sootClass)) {
                        addRelatedMethodWithInfo(trace, signature, mtdSource, depth, edgeSourceMtd);
                        flag = true;
                    }
                    if(sm!=null) {
                        Iterator<SootClass> it2 = sm.getDeclaringClass().getInterfaces().iterator();
                        while (it2.hasNext()) {
                            SootClass interfaceSC = it2.next();
                            for (SootMethod interfaceSM : interfaceSC.getMethods()) {
                                if(interfaceSM.getName().equals(sm.getName())) {
                                    addRelatedMethodWithInfo(trace, interfaceSM.getSignature(), mtdSource, depth, edgeSourceMtd);
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
                getExceptionCallerByParam(edgeSourceMtd, callerHistory, depth + 1, mtdSource, paramIndexCaller, newTrace);
            }
        }
    }

    private void addRelatedMethodWithInfo( List<String> trace, String signature, RelatedMethodSource mtdSource,
                                           int depth,  SootMethod edgeSourceMtd) {
        List<String> newTrace = new ArrayList<>(trace);
        newTrace.add(0, signature);
        RelatedMethod addMethodObj = new RelatedMethod(signature, mtdSource, depth, newTrace);
        addRelatedMethodInstance(edgeSourceMtd, addMethodObj);
    }

    private void addRelatedMethodInstance(SootMethod edgeSource, RelatedMethod addMethod) {
        if(edgeSource.isPublic()) {
            if (edgeSource.getDeclaringClass() == mSootMethod.getDeclaringClass())
                conditionTrackerInfo.addRelatedMethodsInSameClassMap(addMethod);
            else
                conditionTrackerInfo.addRelatedMethodsInDiffClassMap(addMethod);
            conditionTrackerInfo.addRelatedMethods(addMethod.getMethod());
        }
    }


    /**
     * getExceptionCallerByField
     * @param sootMethod
     * @param callerHistory
     * @param depth
     * @param mtdSource
     * @param trace
     */
    private void getExceptionCallerByField(SootMethod sootMethod,HashSet<SootMethod> callerHistory, int depth, RelatedMethodSource mtdSource, List<String> trace) {
        for(SootField field: conditionTrackerInfo.getRelatedFieldValues()){
            for(SootMethod otherMethod: sootMethod.getDeclaringClass().getMethods()){
                if(!otherMethod.hasActiveBody()) continue;
                if(SootUtils.fieldIsChanged(field, otherMethod)){
                    if(otherMethod.isPublic()) {
                        List<String> newTrace = new ArrayList<>(trace);
                        newTrace.add(0,"key field: " + field.toString());
                        newTrace.add(0,otherMethod.getSignature());
                        RelatedMethod addMethod = new RelatedMethod(otherMethod.getSignature(),mtdSource,depth, trace);
                        if(otherMethod.getDeclaringClass() == mSootMethod.getDeclaringClass()) {
                            conditionTrackerInfo.addRelatedMethodsInSameClassMap(addMethod);
                        }
                        else {
                            conditionTrackerInfo.addRelatedMethodsInDiffClassMap(addMethod);
                        }
                        conditionTrackerInfo.addRelatedMethods(otherMethod.getSignature());
                    }
                    List<String> newTrace = new ArrayList<>(trace);
                    newTrace.add(0,"key field: " + field.toString());
                    newTrace.add(0,otherMethod.getSignature());
                    getExceptionCallerByParam(otherMethod, callerHistory,
                            depth+1, RelatedMethodSource.FIELDCALLER, new HashSet<>(), newTrace);
                }
            }
        }
    }

//     /**
//     * get the goto destination of IfStatement
//     */
//    private List<Unit> getGotoTargetsTwice(Body body) {
//        List<Unit> temp = new ArrayList<>();
//        List<Unit> res = new ArrayList<>();
//        for(Unit u : body.getUnits()){
//            if(u instanceof JIfStmt){
//                JIfStmt ifStmt = (JIfStmt)u;
//                if(temp.contains(ifStmt.getTargetBox().getUnit())){
//                    res.add(ifStmt.getTargetBox().getUnit());
//                }else {
//                    temp.add(ifStmt.getTargetBox().getUnit());
//                }
//            }
//            else if(u instanceof GotoStmt){
//                GotoStmt gotoStmt = (GotoStmt)u;
//                if(temp.contains(gotoStmt.getTargetBox().getUnit())){
//                    res.add(gotoStmt.getTargetBox().getUnit());
//                }else {
//                    temp.add(gotoStmt.getTargetBox().getUnit());
//                }
//            }
//        }
//        return res;
//    }



}
