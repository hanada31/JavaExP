package com.iscas.crashtracker.client.crash;

import com.iscas.crashtracker.Main;
import com.iscas.crashtracker.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:00
 * @Version 1.0
 */
@Slf4j
public class JarCrashAnalysisClientTest {
    @org.junit.Test
    public void testConfig() {
        setArgs();
        Main.startAnalyze();
        log.info("JarCrashAnalysisClientTest Finish...\n");
        System.exit(0);

    }

    private void setArgs() {
        String path;
        path = "..\\apk\\";
        path = "D:\\SoftwareData\\dataset\\apk\\android-sdk-project\\";
        String name;
        name = "facebook-android-sdk-905.jar";

        String client = "JarCrashAnalysisClient";

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
    }
}