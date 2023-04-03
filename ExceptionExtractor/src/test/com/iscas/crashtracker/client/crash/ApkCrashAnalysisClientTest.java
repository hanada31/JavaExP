package com.iscas.crashtracker.client.crash;

import com.iscas.crashtracker.Main;
import com.iscas.crashtracker.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 13:59
 * @Version 1.0
 */
@Slf4j
public class ApkCrashAnalysisClientTest {
    @org.junit.Test
    public void testConfig() {
        setArgs();
        Main.startAnalyze();
        System.exit(0);

    }

    private void setArgs() {
        String path;
        path = "D:\\SoftwareData\\dataset\\apk\\Empirical500\\";
//        path = "D:\\SoftwareData\\dataset\\apk\\FanDataICSE2018-before\\";
        path = "..\\app\\";

        String name;
        name = "cgeo.geocaching-4450.apk";

        String client = "ApkCrashAnalysisClient";

        MyConfig.getInstance().setAppName(name);
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultWarpperFolder("..\\results" + File.separator);
        MyConfig.getInstance().setResultFolder(MyConfig.getInstance().getResultWarpperFolder()+ "output" + File.separator);
        MyConfig.getInstance().setTimeLimit(10);
        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_apk);
        MyConfig.getInstance().setStrategy("");
//        MyConfig.getInstance().setStrategy("NoRelatedMethodFilter");
//        MyConfig.getInstance().setAndroidOSVersion("2.3");

        MyConfig.getInstance().setCrashInfoFilePath("..\\Files\\crashInfo.json");
        MyConfig.getInstance().setExceptionFolderPath("..\\Files\\");
//        MyConfig.getInstance().setCallgraphAlgorithm("SPARK");
//        MyConfig.getInstance().setCallgraphAlgorithm("CHA");


    }

}