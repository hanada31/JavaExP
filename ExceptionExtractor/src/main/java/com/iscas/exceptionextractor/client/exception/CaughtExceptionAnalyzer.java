package com.iscas.exceptionextractor.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.exceptionextractor.model.analyzeModel.ExceptionInfo;
import com.iscas.exceptionextractor.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/23 14:39
 * @Version 1.0
 */
@Slf4j
public class CaughtExceptionAnalyzer extends ExceptionAnalyzer {
    List<ExceptionInfo> caughtExceptionInfoList;
    public List<ExceptionInfo> getCaughtExceptionInfoList() {
        return caughtExceptionInfoList;
    }


    @Override
    public void analyze() {
        log.info("getCaughtExceptionList start...");
        getCaughtExceptionList();
        outputCaughtExceptionInfo();
        log.info("getCaughtExceptionList end...");
    }

    /**
     * analyze the information of caught exceptions
     */
    private void getCaughtExceptionList() {
        caughtExceptionInfoList = new ArrayList<>();
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if(openFilter && filterMethod(sootMethod)) continue;
                if (sootMethod.hasActiveBody()) {
                    try {
                        getCaughtUnitWithType(sootMethod,new HashSet<>());
                    } catch (Exception |  Error e) {
                        log.info("Exception |  Error:::" + sootMethod.getSignature());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void outputCaughtExceptionInfo() {
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfCaughtException(caughtExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"caughtException");
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

}
