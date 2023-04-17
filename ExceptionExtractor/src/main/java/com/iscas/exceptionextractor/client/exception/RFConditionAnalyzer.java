//package com.iscas.exceptionextractor.client.exception;
//
//import com.iscas.exceptionextractor.model.analyzeModel.AppModel;
//import com.iscas.exceptionextractor.model.analyzeModel.RFCondition;
//import com.iscas.exceptionextractor.model.sootAnalysisModel.Context;
//import com.iscas.exceptionextractor.utils.*;
//import soot.Body;
//import soot.SootMethod;
//import soot.Unit;
//import soot.Value;
//import soot.jimple.InvokeExpr;
//import soot.jimple.internal.AbstractInstanceInvokeExpr;
//import soot.jimple.internal.JAssignStmt;
//import soot.jimple.internal.JIfStmt;
//import soot.jimple.internal.JStaticInvokeExpr;
//import soot.shimple.PhiExpr;
//import soot.toolkits.scalar.UnitValueBoxPair;
//
//import java.util.*;
//
///**
// * @Author hanada
// * @Date 2023/4/13 18:26
// * @Version 1.0
//// */
//public class RFConditionAnalyzer {
//    AppModel appModel;
//
//    private Set<String> getAttriStr(SootMethod sm, Unit u, String attr_type) {
//        boolean beCompared = false;
//        Set<String> resSet = new HashSet<String>();
//        // get its string
//        List<UnitValueBoxPair> use_var_list = SootUtils.getUseOfLocal(sm.getSignature(),u);
//        Map<List, String> todoUnit2Condition = new HashMap<List,String>();
//        // get obtained data may be transfered first before compare
//        resSet.addAll(completeTodoList("", u, use_var_list, todoUnit2Condition, new HashSet<Unit>(), attr_type));
//        todoUnit2Condition.put(use_var_list, "");
//        for (Map.Entry<List, String> en: todoUnit2Condition.entrySet()) {
//            String condition = en.getValue();
//            List<UnitValueBoxPair> unitList = en.getKey();
//            for(UnitValueBoxPair  useUnitBox: unitList){
//                Unit useUnit = useUnitBox.getUnit();
//                Value inputVar = useUnitBox.getValueBox().getValue();
//                // ============bool type==========================
//                if (useUnit instanceof JAssignStmt) {
//                    if (useUnit.toString().contains("contains")) {
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "contains" , condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    }
//                    else if (useUnit.toString().contains("equals") || useUnit.toString().contains("contentEquals")) { // equals
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "equals", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    }
//                    else if (useUnit.toString().contains("startsWith")) {
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "startsWith", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    } else if (useUnit.toString().contains("endsWith")) {
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "endsWith", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    } else if (useUnit.toString().contains("isEmpty")) {
//                        beCompared = true;
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type, "Empty",
//                                "isEmpty", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.add("\"notEmpty\"");
//                    }
//                }
//                else if (useUnit instanceof JIfStmt) {
//                    if (useUnit.toString().contains("!= null")) {
//                        beCompared = true;
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type, "null",
//                                "nullChecker", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.add("\"notEmpty\"");
//                    }
//                    else if (useUnit.toString().contains("== null")) {
//                        beCompared = true;
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type, "null",
//                                "nullChecker", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.add("\"notEmpty\"");
//                    }
//                    else if (useUnit.toString().contains("== ") ) { // equals
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "equals", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    }
//                    else if (useUnit.toString().contains("!= ") ) { // equals
//                        beCompared = true;
//                        Set<String> candidates = getValueofUnit2Set(useUnit, inputVar, attr_type);
//                        RFCondition attr = new RFCondition(sm.getSignature() + "," + useUnit.toString()+","+useUnit.hashCode(), attr_type,
//                                PrintUtils.printSet(candidates), "equals", condition);
//                        CollectionUtils.add_rfcondition_to_map(attr.id, attr, appModel.unit2RFCondition);
//                        resSet.addAll(candidates);
//                    }
//                }
//
//            }
//        }
//        return resSet;
//    }
//
//    private Set<String> completeTodoList(String condition, Unit defUnit, List<UnitValueBoxPair> use_var_list,
//                                         Map<List, String> todoUnit2Condition, HashSet<Unit> historySet, String attr_type) {
//        Set<String> res = new HashSet<String>();
//        List<UnitValueBoxPair> resList = getComparedUnit(use_var_list);
//        todoUnit2Condition.put(resList, condition);
//        for (UnitValueBoxPair it : use_var_list) {
//            Unit useUnit = it.getUnit();
//            if (historySet.contains(useUnit))
//                continue;
//            else
//                historySet.add(useUnit);
//            String newCon = getConditionFromUnit(useUnit);
//            if (newCon!=null) {
//                res.addAll(completeTodoList(condition+","+newCon, useUnit, SootUtils.getUseOfLocal(useUnit), todoUnit2Condition, historySet, attr_type));
//            } else {
//                InvokeExpr invoke = SootUtils.getInvokeExp(useUnit);
//                if (invoke == null)
//                    continue;
//                String invoke_Signature = invoke.getMethod().getSignature();
//                List<Body> bodys = SootUtils.getBodys(target_name, invoke.getMethod(),appModel);
//                for(Body b :bodys){
//                    // String action = intent.getAction();
//                    // Utils.handleAction(action);
//                    for (int i = 0; i < invoke.getArgs().size(); i++) {
//                        Value arg = invoke.getArg(i);
//                        if (arg == defUnit.getDefBoxes().get(0).getValue()) {
//                            for (Unit unit_in_body : b.getUnits()) {
//                                if (unit_in_body.toString().contains("@parameter" + i)) {
//                                    if(!history.contains(b.getMethod().getSignature())){
//                                        ValueObtainer vo = new ValueObtainer(appModel, invoke_Signature, ConstantUtils.PARJAVA,
//                                                contextsValue,target_name, counter);
//                                        Context contexts = vo.getContextValue(useUnit, invoke, b.getMethod(), method_name);
//                                        history += b.getMethod().getSignature();
//                                        IntentAnalyzer transferM = new IntentAnalyzer(appModel, b, target_name, contexts, history ,counter);
//                                        res.addAll(transferM.getAttriStr(unit_in_body, attr_type));
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return res;
//    }
//
//    private String getConditionFromUnit(Unit useUnit) {
//        if(useUnit instanceof PhiExpr) return "Phi";
//        else if(useUnit.toString().contains("toString(") ) return "";
//        else if(useUnit.toString().contains("valueOf(") ) return "";
//        else if(useUnit.toString().contains("toLowerCase(") ) return "";
//        else if(useUnit.toString().contains("toUpperCase(") ) return "";
//        else if(useUnit.toString().contains("trim(") ) return "";
//        else if(useUnit.toString().contains("substring(") ){
//            if(useUnit instanceof JAssignStmt){
//                JAssignStmt jas1 = (JAssignStmt) useUnit;
//                AbstractInstanceInvokeExpr invokeStmt = (AbstractInstanceInvokeExpr) jas1.getRightOp();
//                ValueObtainer vo = new ValueObtainer(appModel, method_name, ConstantUtils.FLAGATTRI, contextsValue,target_name,counter);
//                int b = 0, e = 0;
//                if (vo.getValueofVar(invokeStmt.getArg(0),useUnit, 0).getValues().size() > 0){
//                    String str_b = vo.getValueofVar(invokeStmt.getArg(0),useUnit, 0).getValues().get(0);
//                    if (StringUtils.isInteger(str_b)) {
//                        b = Integer.parseInt(str_b);
//                        if (invokeStmt.getArgCount() == 1) {
//                            return "substring "+b;
//                        }
//                        if (invokeStmt.getArgCount() == 2) {
//                            if (vo.getValueofVar(invokeStmt.getArg(1), useUnit, 0).getValues().size() > 0){
//                                String str_e = vo.getValueofVar(invokeStmt.getArg(1), useUnit, 0).getValues().get(0);
//                                if (StringUtils.isInteger(str_e)) {
//                                    e = Integer.parseInt(str_e);
//                                    return "substring "+b+" "+(e-b);
//                                }
//                            }
//                        }
//                    }
//                }
//                return "";
//            }
//        }
//        else if(useUnit.toString().contains("charAt(") ){
//            JAssignStmt jas1 = (JAssignStmt) useUnit;
//            AbstractInstanceInvokeExpr invokeStmt = (AbstractInstanceInvokeExpr) jas1.getRightOp();
//            ValueObtainer vo = new ValueObtainer(appModel, method_name, ConstantUtils.FLAGATTRI, contextsValue,target_name, counter);
//            int b = 0;
//            if (vo.getValueofVar(invokeStmt.getArg(0),useUnit).getValues().size() > 0){
//                String str_b = vo.getValueofVar(invokeStmt.getArg(0),useUnit).getValues().get(0);
//                if (StringUtils.isInteger(str_b)) {
//                    b = Integer.parseInt(str_b);
//                    if (invokeStmt.getArgCount() == 1) {
//                        return "charAt "+b;
//                    }
//                }
//            }
//            return "";
//        }else if(useUnit.toString().contains("concat(") ){
//            JAssignStmt jas1 = (JAssignStmt) useUnit;
//            AbstractInstanceInvokeExpr invokeStmt = (AbstractInstanceInvokeExpr) jas1.getRightOp();
//            ValueObtainer vo = new ValueObtainer(appModel, method_name, ConstantUtils.FLAGATTRI, contextsValue,target_name, counter);
//            List<String> vals = vo.getValueofVar(invokeStmt.getArg(0),useUnit, 0).getValues();
//            if (vals.size() > 0){
//                String str =vals.get(0);
//                return "concat "+str;
//            }
//            return "";
//        }
//        return null;
//    }
//
//    private List<UnitValueBoxPair> getComparedUnit(List<UnitValueBoxPair> use_var_list) {
//        List<UnitValueBoxPair> resList = new ArrayList<UnitValueBoxPair>();
//        for (UnitValueBoxPair ubp : use_var_list) {
//            Unit u = ubp.getUnit();
//            for (String s : ConstantUtils.compared_methods) {
//                if (u.toString().contains(s)) {
//                    resList.add(ubp);
//                    break;
//                }
//            }
//        }
//        return resList;
//    }
//
//
//    Set<String> getValueofUnit2Set(Unit useUnit, Value inputVar, String attr_type) {
//        Set<String> resSet = new HashSet<String>();
//        if (useUnit instanceof JAssignStmt) {
//            // action equals xxx or xxx equals action, make sure which var is
//            // target
//            JAssignStmt jas1 = (JAssignStmt) useUnit;
//            Value strVal = null;
//            if (jas1.getRightOp() instanceof AbstractInstanceInvokeExpr) {
//                AbstractInstanceInvokeExpr invokeStmt = (AbstractInstanceInvokeExpr) jas1.getRightOp();
//                if (invokeStmt.getArg(0).equals(inputVar)) // actrionVar equals
//                    // xxx
//                    strVal = invokeStmt.getBase();
//                else // xxx equals actrionVar
//                    strVal = invokeStmt.getArg(0);
//            } else if (jas1.getRightOp() instanceof JStaticInvokeExpr) {
//                JStaticInvokeExpr invokeStmt = (JStaticInvokeExpr) jas1.getRightOp();
//                if (invokeStmt.getArg(0).equals(inputVar)) // actrionVar equals
//                    // xxx
//                    strVal = invokeStmt.getArg(1);
//                else // xxx equals actrionVar
//                    strVal = invokeStmt.getArg(0);
//            }
//            ValueObtainer vo = new ValueObtainer(appModel, method_name, ConstantUtils.FLAGATTRI, contextsValue,target_name, counter);
//            for (String res : vo.getValueofVar(strVal, useUnit,0).getValues())
//                resSet.add("\"" + res + "\"");
//        } else if (useUnit instanceof JIfStmt) {
//            JIfStmt jif = (JIfStmt) useUnit;
//            if (jif.getCondition().getUseBoxes().size() > 1) {
//                String res = jif.getCondition().getUseBoxes().get(1).getValue().toString();
//                resSet.add("\"" + res + "\"");
//            }
//        }
//        return resSet;
//    }
//
//}
