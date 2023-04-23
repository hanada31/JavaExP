package com.iscas.exceptionextractor.client.exception;

import com.iscas.exceptionextractor.base.Analyzer;
import lombok.extern.slf4j.Slf4j;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;
/**
 * @Author hanada
 * @Date 2022/3/11 15:21
 * @Version 1.0
 */
@Slf4j
public class ExceptionAnalyzer extends Analyzer {

    // for debugging only
    boolean openFilter = false;
    boolean filterMethod(SootMethod sootMethod) {
        List<String> mtds = new ArrayList<>();
        mtds.add("throw_with_modified_value_condition2");
        for(String tag: mtds){
            if (sootMethod.getSignature().contains(tag)) {
                return false;
            }
        }
        return true;
    }


    public ExceptionAnalyzer() {
        super();
    }

    @Override
    public void analyze() {
        DeclaredExceptionAnalyzer declaredExceptionAnalyzer = new DeclaredExceptionAnalyzer();
        declaredExceptionAnalyzer.analyze();

        CaughtExceptionAnalyzer caughtExceptionAnalyzer = new CaughtExceptionAnalyzer();
        caughtExceptionAnalyzer.analyze();

        ThrownExceptionAnalyzer thrownExceptionAnalyzer = new ThrownExceptionAnalyzer();
        thrownExceptionAnalyzer.analyze();

    }







}
