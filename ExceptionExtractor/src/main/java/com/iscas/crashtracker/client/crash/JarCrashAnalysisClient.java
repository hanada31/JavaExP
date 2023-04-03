package com.iscas.crashtracker.client.crash;

import com.iscas.crashtracker.base.Analyzer;
import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.BaseClient;
import com.iscas.crashtracker.client.cg.cgJava.CallGraphofJavaClient;
import com.iscas.crashtracker.client.soot.SootAnalyzer;
import com.iscas.crashtracker.utils.ConstantUtils;
import com.iscas.crashtracker.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;

/**
 * @Author hanada
 * @Date 2022/3/22 20:04
 * @Version 1.0
 */
@Slf4j
public class JarCrashAnalysisClient extends BaseClient {
    @Override
    protected void clientAnalyze() {
        if (!MyConfig.getInstance().isSootAnalyzeFinish()) {
            SootAnalyzer sootAnalyzer = new SootAnalyzer();
            sootAnalyzer.analyze();
        }
        if (!MyConfig.getInstance().isCallGraphAnalyzeFinish()) {
            ConstantUtils.CGANALYSISPREFIX = Global.v().getAppModel().getPackageName();
            new CallGraphofJavaClient().start();
            MyConfig.getInstance().setCallGraphAnalyzeFinish(true);
        }

        log.info("Start analyze with CrashAnalysisClient.");
        Analyzer analyzer = new CrashAnalysis();
        analyzer.analyze();
        log.info("Successfully analyze with CrashAnalysisClient.");
        //        PackManager.v().writeOutput();

    }

    @Override
    public void clientOutput() throws IOException, DocumentException {
        String summary_app_dir = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName()
                + File.separator;
        FileUtils.createFolder(summary_app_dir);

        CrashAnalysisClientOutput outer = new CrashAnalysisClientOutput();
        outer.writeToJson(summary_app_dir+Global.v().getAppModel().getAppName()+".json", Global.v().getAppModel().getCrashInfoList());

    }
}
