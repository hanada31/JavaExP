package com.iscas.exceptionextractor.client.exception;

import com.iscas.exceptionextractor.Main;
import com.iscas.exceptionextractor.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

import java.io.File;

/**
 * @Author hanada
 * @Date 2022/9/8 14:02
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoClientTest {
    String[] versions = {"2.3", "4.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"};

    @org.junit.Test
    public void testConfig() {
        log.info("ExceptionInfoClientTest start...");
        setArgs();
        Main.startAnalyze();
        log.info("ExceptionInfoClientTest Finish...");
        System.exit(0);
    }

    private void setArgs() {
        String path, androidVersion;
        path = "..\\M_framework\\";
//        path = "D:\\SoftwareData\\dataset\\android-framework\\classes\\";
        MyConfig.getInstance().setFileVersion(versions[0]);
        MyConfig.getInstance().setFileVersion("0.1");
        String client = "ExceptionInfoClient";

        androidVersion = "android"+MyConfig.getInstance().getFileVersion();
//        androidVersion = "jdk1.8\\tools"+MyConfig.getInstance().getFileVersion();
//        androidVersion = "jdk1.8\\nashorn"+MyConfig.getInstance().getFileVersion();
        androidVersion = "hand"+MyConfig.getInstance().getFileVersion();
        MyConfig.getInstance().setAppName(androidVersion);
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        MyConfig.getInstance().setTimeLimit(100);
        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_only_class);
        MyConfig.getInstance().setFileSuffixLength(0);
        String androidFolder = MyConfig.getInstance().getResultFolder() +File.separator+androidVersion+File.separator;
        MyConfig.getInstance().setExceptionFilePath(androidFolder+"exceptionInfo"+File.separator);
        MyConfig.getInstance().setPermissionFilePath(androidFolder+"Permission"+File.separator+"permission.txt");
        MyConfig.getInstance().setAndroidCGFilePath(androidFolder+"CallGraphInfo"+File.separator+"cg.txt");
        MyConfig.getInstance().setJimple(false);
    }
}
