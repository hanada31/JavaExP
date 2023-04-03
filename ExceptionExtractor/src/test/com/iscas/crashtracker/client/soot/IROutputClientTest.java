package com.iscas.crashtracker.client.soot;

import com.iscas.crashtracker.Main;
import com.iscas.crashtracker.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:04
 * @Version 1.0
 */
@Slf4j
public class IROutputClientTest {
    @org.junit.Test
    public void testConfig() {
        setArgs();
        Main.startAnalyze();
        log.info("IROutputClientTest Finish...\n");
        System.exit(0);
    }

    private void setArgs() {
        String path;
        path = "D:\\SoftwareData\\dataset\\apk\\Empirical500\\";
        path = "D:\\SoftwareData\\dataset\\android-framework\\classes\\";
//        path = "D:\\SoftwareData\\dataset\\apk\\FanDataICSE2018-before\\";
        path = "..\\apk\\";
        String name;
        name = "cgeo.geocaching-4450.apk";

        String client = "IROutputClient";

        MyConfig.getInstance().setAppName(name);
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultWarpperFolder("..\\results" + File.separator);
        MyConfig.getInstance().setResultFolder(MyConfig.getInstance().getResultWarpperFolder()+ "output" + File.separator);
        MyConfig.getInstance().setTimeLimit(10);
        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_apk);
        MyConfig.getInstance().setFileSuffixLength(4);
    }
}