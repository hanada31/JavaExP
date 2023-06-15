package com.iscas.JavaExP.client.exception;

import com.alibaba.fastjson.JSONArray;
import com.iscas.JavaExP.model.analyzeModel.ExceptionInfo;
import com.iscas.JavaExP.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2023/4/23 14:38
 * @Version 1.0
 */
@Slf4j
public class DeclaredExceptionAnalyzer extends ExceptionAnalyzer {
    List<ExceptionInfo> declaredExceptionInfoList = new ArrayList<>();

    @Override
    public void analyze() {
        log.info("getDeclaredExceptionList start...");
        getDeclaredExceptionList();
        log.info("getDeclaredExceptionList end...");
    }

    public List<ExceptionInfo> getDeclaredExceptionInfoList() {
        return declaredExceptionInfoList;
    }

    /**
     * analyze the information of declared exceptions
     */
    private void getDeclaredExceptionList() {
        for (SootClass sootClass : SootUtils.getApplicationClasses()) {
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setExceptionName(sootClass.getName());
            if(!exceptionInfo.findExceptionType(sootClass)) continue;
            declaredExceptionInfoList.add(exceptionInfo);
        }
        JSONArray exceptionListElement  = new JSONArray(new ArrayList<>());
        ExceptionInfoClientOutput.getSummaryJsonArrayOfDeclaredException(declaredExceptionInfoList, exceptionListElement);
        ExceptionInfoClientOutput.writeExceptionSummaryInJson(exceptionListElement,"declaredException");
    }

}
