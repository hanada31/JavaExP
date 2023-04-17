package com.iscas.exceptionextractor.client.exception;

import com.iscas.exceptionextractor.base.Analyzer;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.client.BaseClient;
import com.iscas.exceptionextractor.client.soot.SootAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;

import java.io.IOException;

/**
 * @Author hanada
 * @Date 2022/3/11 15:03
 * @Version 1.0
 */

@Slf4j
public class ExceptionInfoClient extends BaseClient {
    /**
     * analyze logic for single app
     *
     * @return
     */
    @Override
    protected void clientAnalyze() {
        if (!MyConfig.getInstance().isSootAnalyzeFinish()) {
            log.info("SootAnalyzer start...");
            SootAnalyzer sootAnalyzer = new SootAnalyzer();
            sootAnalyzer.analyze();
        }
//        if (!MyConfig.getInstance().isCallGraphAnalyzeFinish()) {
//            log.info("CallGraphofJavaClient start...");
//            ConstantUtils.CGANALYSISPREFIX = ConstantUtils.FRAMEWORKPREFIX;
//            new CallGraphofJavaClient().start();
//            MyConfig.getInstance().setCallGraphAnalyzeFinish(true);
//        }

        log.info("ExceptionAnalyzer start...");
        Analyzer analyzer = new ExceptionAnalyzer();
        analyzer.analyze();
        log.info("Successfully analyze with ExceptionInfoClient.");
    }

    @Override
    public void clientOutput() throws IOException, DocumentException {
//        PackManager.v().writeOutput();
//        String summary_app_dir = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName()
//                + File.separator;
//        FileUtils.createFolder(summary_app_dir);
//
//        ExceptionInfoClientOutput.writeToJson(summary_app_dir+Global.v().getAppModel().getAppName()+".json", Global.v().getAppModel().getExceptionInfoList());
//
    }
}
