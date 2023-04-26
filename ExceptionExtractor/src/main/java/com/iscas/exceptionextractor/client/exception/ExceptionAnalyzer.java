package com.iscas.exceptionextractor.client.exception;

import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.base.MyConfig;
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
    boolean isInterProcedure = true;

    public ExceptionAnalyzer() {
        super();
        if(MyConfig.getInstance().getTag()!=null)
           openFilter = true;
    }

    boolean filterMethod(SootMethod sootMethod) {
        List<String> mtds = new ArrayList<>();
//        mtds.add("com.arialyy.aria.core.event.EventMsgUtil$1: void run()");
        if(MyConfig.getInstance().getTag()!=null){
            mtds.add(MyConfig.getInstance().getTag());
        }
        for(String tag: mtds){
            if (sootMethod.getSignature().contains(tag)) {
                return false;
            }
        }

        return true;
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
