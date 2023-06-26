package com.iscas.JavaExP.client.exception;

import com.iscas.JavaExP.Main;
import com.iscas.JavaExP.base.MyConfig;
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

    @org.junit.Test
    public void testConfig() {
        log.info("ExceptionInfoClientTest start...");
        setArgs();
        Main.startAnalyze();
        log.info("ExceptionInfoClientTest Finish...");
        System.exit(0);
    }

    private void setArgs() {
        String client = "ExceptionInfoClient";
        String path;

        path = "..\\Benchmark\\";
//        path = "D:\\SoftwareData\\dataset\\android-framework\\classes";
        String targetName = "jdk1.8";
//        targetName = "testcase1.0";

        MyConfig.getInstance().setAppName(targetName);
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        MyConfig.getInstance().setTimeLimit(300);
//        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_only_class);
        MyConfig.getInstance().setFileSuffixLength(0);
        String targetFolder = MyConfig.getInstance().getResultFolder() +File.separator+targetName+File.separator;
        MyConfig.getInstance().setExceptionFilePath(targetFolder+"exceptionInfo"+File.separator);
        MyConfig.getInstance().setJimple(false);
    }
}
