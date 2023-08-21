package com.iscas.JavaExP.client.exception;

import com.google.common.collect.Lists;
import com.iscas.JavaExP.base.Analyzer;
import com.iscas.JavaExP.utils.SootUtils;
import com.iscas.JavaExP.utils.StringUtils;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2023/4/18 9:58
 * @Version 1.0
 */
public class RegularExpressionAnalyzer  extends Analyzer {
    private String exceptionMsg = "[\\s\\S]*";
    private SootMethod mSootMethod;
    private Unit mUnit;
    private String mExceptionName;

    public RegularExpressionAnalyzer(SootMethod sootMethod, Unit unit, String exceptionName) {
        mSootMethod = sootMethod;
        mUnit = unit;
        mExceptionName = exceptionName;
    }

    @Override
    public void analyze() {
        getExceptionMessage(mSootMethod, mUnit, new ArrayList<>());

    }
    /**
     * get the msg info for an ExceptionInfo
     */
    String getExceptionMessage(SootMethod sootMethod, Unit unit,  List<Integer> times){
        Body body = sootMethod.getActiveBody();
        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
        times.add(1);
        if (times.size() > 50) {
            return exceptionMsg;
        }
        if(unit instanceof ThrowStmt) {
            List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(), ((ThrowStmt) unit).getOp(), unit);
            Unit defUnit = defsOfOps.get(0);
            InvokeExpr invoke = SootUtils.getInvokeExp(defUnit);
            if(invoke!=null) {
                List<Unit> retList = SootUtils.getRetList(invoke.getMethod());
                for (Unit retU : retList) {
                    Value val = ((JReturnStmt) retU).getOp();
                    if (val instanceof Local) {
                        getExceptionMessage(invoke.getMethod(), retU, times);
                    }
                }
            }
        }
        List<Unit> predsOf = unitGraph.getPredsOf(unit);
        for (Unit predUnit : predsOf) {
            if (predUnit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) predUnit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                if (invokeExpr.getMethod().getDeclaringClass().toString().equals(mExceptionName)) {
                    // 可能初始化会有多个参数，只关注第一个String参数
                    if (invokeExpr.getArgCount() > 0 && StringUtils.isStringType(invokeExpr.getArgs().get(0).getType())) {
                        Value arg = invokeExpr.getArgs().get(0);
                        if (arg instanceof Local) {
                            List<String> message = Lists.newArrayList();
                            message.add("");
                            getMsgContentByTracingValue(sootMethod, (Local) arg, predUnit, message);
                            exceptionMsg = addQeSymbolToMessage(message.get(0));
                        } else if (arg instanceof Constant) {
                            StringConstant arg1 = (StringConstant) arg;
                            exceptionMsg = addQeSymbolToMessage(arg1.value);
                        }
                    }
                } else {
                    getExceptionMessage(sootMethod, predUnit, times);
                }
            }
        }
        return exceptionMsg;
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
    private void getMsgContentByTracingValue( SootMethod sootMethod, Local localTemp, Unit unit, List<String> message){
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
                        getMsgContentByTracingValue( sootMethod, (Local) value, defOfLocal, message);
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
                        getMsgContentByTracingValue(sootMethod, (Local) value, defOfLocal, message);
                    }
                }
            } else if (rightOp instanceof NewExpr) {
                NewExpr rightOp1 = (NewExpr) rightOp;
                if (rightOp1.getBaseType().toString().equals("java.lang.StringBuilder")) {
                    traceStringBuilderBack(sootMethod, defOfLocal, message, 0);
                }
            } else if (rightOp instanceof Local) {
                getMsgContentByTracingValue( sootMethod, (Local) rightOp, defOfLocal, message ) ;
            }
        }
    }

    /**
     * traceStringBuilderBack
     */
    private void traceStringBuilderBack(SootMethod sootMethod, Unit unit, List<String> message, int index){
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
            traceStringBuilderBack(sootMethod, succs, message, index + 1);
        }
    }

    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public void setExceptionMsg(String exceptionMsg) {
        this.exceptionMsg = exceptionMsg;
    }


//    /**
//     * get the msg info for an ExceptionInfo
//     */
//    void getExceptionMessage(SootMethod sootMethod, Unit unit, ExceptionInfo exceptionInfo, List<Integer> times){
//        Body body = sootMethod.getActiveBody();
//        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
//        String exceptionClassName = exceptionInfo.getExceptionName();
//        times.add(1);
//        if (times.size() > 50) {
//            return;
//        }
//        if(unit instanceof ThrowStmt) {
//            List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(), ((ThrowStmt) unit).getOp(), unit);
//            Unit defUnit = defsOfOps.get(0);
//            InvokeExpr invoke = SootUtils.getInvokeExp(defUnit);
//            if(invoke!=null) {
//                List<Unit> retList = SootUtils.getRetList(invoke.getMethod());
//                for (Unit retU : retList) {
//                    Value val = ((JReturnStmt) retU).getOp();
//                    if (val instanceof Local) {
//                        getExceptionMessage(invoke.getMethod(), retU, exceptionInfo, times);
//                    }
//                }
//            }
//        }
//        List<Unit> predsOf = unitGraph.getPredsOf(unit);
//        for (Unit predUnit : predsOf) {
//            if (predUnit instanceof InvokeStmt) {
//                InvokeStmt invokeStmt = (InvokeStmt) predUnit;
//                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
//                if (invokeExpr.getMethod().getDeclaringClass().toString().equals(exceptionClassName)) {
//                    // 可能初始化会有多个参数，只关注第一个String参数
//                    if (invokeExpr.getArgCount() > 0 && StringUtils.isStringType(invokeExpr.getArgs().get(0).getType())) {
//                        Value arg = invokeExpr.getArgs().get(0);
//                        if (arg instanceof Local) {
//                            List<String> message = Lists.newArrayList();
//                            message.add("");
//                            getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) arg, unit, message);
//                            String exceptionMsg = addQeSymbolToMessage(message.get(0));
//                            exceptionInfo.setExceptionMsg(exceptionMsg);
//                        } else if (arg instanceof Constant) {
//                            StringConstant arg1 = (StringConstant) arg;
//                            String exceptionMsg = addQeSymbolToMessage(arg1.value);
//                            exceptionInfo.setExceptionMsg(exceptionMsg);
//                        }
//                    }
//                } else {
//                    getExceptionMessage(sootMethod, predUnit, exceptionInfo,times);
//                }
//            }
//        }
//    }
//
//    private String addQeSymbolToMessage(String input) {
//        String exceptionMsg = "";
//        String[] ss =input.split("\\Q[\\s\\S]*\\E");
//        for(int i= 0; i<ss.length-1;i++){
//            exceptionMsg+="\\Q"+ss[i]+"\\E"+"[\\s\\S]*";
//        }
//        if(ss.length>=1)
//            exceptionMsg+="\\Q"+ss[ss.length-1]+"\\E";
//        if(input.endsWith("[\\s\\S]*"))
//            exceptionMsg+="[\\s\\S]*";
//
//        String temp = "";
//        while(!exceptionMsg.equals(temp)) {
//            temp= exceptionMsg;
//            exceptionMsg = exceptionMsg.replace("\\Q\\E", "");
//            exceptionMsg = exceptionMsg.replace("\\E\\Q", "");
//            exceptionMsg = exceptionMsg.replace("[\\s\\S]*[\\s\\S]*", "[\\s\\S]*");
//        }
//        return exceptionMsg;
//    }
//
//
//    /**
//     * getMsgContentByTracingValue
//     */
//    private void getMsgContentByTracingValue(ExceptionInfo exceptionInfo, SootMethod sootMethod, Local localTemp, Unit unit, List<String> message){
//        List<Unit> defsOfOps = SootUtils.getDefOfLocal(sootMethod.getSignature(),localTemp, unit);
//        if(defsOfOps==null || defsOfOps.size()==0) return;
//        Unit defOfLocal = defsOfOps.get(0);
//        if (defOfLocal.equals(unit)) {
//            return;
//        }
//        if (defOfLocal instanceof DefinitionStmt) {
//            Value rightOp = ((DefinitionStmt) defOfLocal).getRightOp();
//            if (rightOp instanceof Constant) {
//                String s = message.get(0) + rightOp;
//                message.set(0,s);
//            } else if (rightOp instanceof InvokeExpr) {
//                InvokeExpr invokeExpr = (InvokeExpr) rightOp;
//                String invokeSig = invokeExpr.getMethod().getSignature();
//                if (invokeSig.equals("<java.lang.StringBuilder: java.lang.String toString()>")) {
//                    Value value = invokeExpr.getUseBoxes().get(0).getValue();
//                    if (value instanceof Local) {
//                        getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) value, unit, message);
//                    }
//                } else if (invokeSig.startsWith("<java.lang.StringBuilder: java.lang.StringBuilder append")) {
//                    Value argConstant = invokeExpr.getArgs().get(0);
//                    String s;
//                    if (argConstant instanceof Constant) {
//                        if (argConstant instanceof StringConstant) {
//                            String value = ((StringConstant) argConstant).value;
//                            s = value + message.get(0);
//                        } else {
//                            s = argConstant + message.get(0);
//                        }
//
//                    } else {
//                        s = "[\\s\\S]*" + message.get(0) ;
//                        //add this as related value
////                        List<Unit> allPreds = new ArrayList<>();
////                        SootUtils.getAllPredsofUnit(sootMethod, defOfLocal,allPreds);
////                        allPreds.add(defOfLocal);
////                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, defOfLocal, argConstant, new ArrayList<>(), new HashSet<>(), true);
//                    }
//                    message.set(0, s);
//
//                    Value value = ((JVirtualInvokeExpr) invokeExpr).getBaseBox().getValue();
//                    if (value instanceof Local) {
//                        getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) value, unit, message);
//                    }
//                }
//            } else if (rightOp instanceof NewExpr) {
//                NewExpr rightOp1 = (NewExpr) rightOp;
//                if (rightOp1.getBaseType().toString().equals("java.lang.StringBuilder")) {
//                    traceStringBuilderBack(exceptionInfo, sootMethod, defOfLocal, message, 0);
//                }
//            } else if (rightOp instanceof Local) {
//                getMsgContentByTracingValue(exceptionInfo, sootMethod, (Local) rightOp, unit, message ) ;
//            }
//        }
//    }
//
//    /**
//     * traceStringBuilderBack
//     */
//    private void traceStringBuilderBack(ExceptionInfo exceptionInfo, SootMethod sootMethod, Unit unit, List<String> message, int index){
//        if (index > 10) {
//            return;
//        }
//        Body body = sootMethod.getActiveBody();
//        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
//        List<Unit> succsOf = unitGraph.getSuccsOf(unit);
//        for (Unit succs : succsOf) {
//            if (succs instanceof InvokeStmt) {
//                InvokeExpr invokeExpr = ((InvokeStmt) succs).getInvokeExpr();
//                String invokeSig = invokeExpr.getMethod().getSignature();
//                if (invokeSig.startsWith("<java.lang.StringBuilder: java.lang.StringBuilder append")) {
//                    Value argConstant = invokeExpr.getArgs().get(0);
//                    String s;
//                    if (argConstant instanceof Constant) {
//                        if (argConstant instanceof StringConstant) {
//                            String value = ((StringConstant) argConstant).value;
//                            s = message.get(0) + value;
//                        } else {
//                            s = message.get(0) + argConstant;
//                        }
//                    } else{
//                        s = message.get(0) + "[\\s\\S]*";
//                        //add this as related value
////                        List<Unit> allPreds = new ArrayList<>();
////                        SootUtils.getAllPredsofUnit(sootMethod, succs,allPreds);
////                        allPreds.add(succs);
////                        extendRelatedValues(conditionWithValueSet, sootMethod, allPreds, exceptionInfo, succs, argConstant, new ArrayList<>(), new HashSet<>(), true);
//                    }
//                    message.set(0, s);
//                }
//            } else if (succs instanceof ThrowStmt) {
//                return;
//            }
//            traceStringBuilderBack(exceptionInfo, sootMethod, succs, message, index + 1);
//        }
//    }

}
