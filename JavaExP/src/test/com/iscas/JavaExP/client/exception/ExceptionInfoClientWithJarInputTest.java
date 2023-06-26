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
public class ExceptionInfoClientWithJarInputTest {

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

        path = "..\\M_framework\\apk\\";
//        path = "D:\\SoftwareData\\dataset\\android-framework\\classes\\";
        String autName = "iflyrectj-6.0.3682-30010009";

        MyConfig.getInstance().setAppName(autName+".apk");
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        MyConfig.getInstance().setTimeLimit(100);
        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_only_class);
        MyConfig.getInstance().setFileSuffixLength(0);
        String autFolder = MyConfig.getInstance().getResultFolder() +File.separator+autName+File.separator;
        MyConfig.getInstance().setExceptionFilePath(autFolder+"exceptionInfo"+File.separator);
        MyConfig.getInstance().setPermissionFilePath(autFolder+"Permission"+File.separator+"permission.txt");
        MyConfig.getInstance().setAndroidCGFilePath(autFolder+"CallGraphInfo"+File.separator+"cg.txt");
        MyConfig.getInstance().setJimple(false);
//        MyConfig.getInstance().setTag("com.ifly");
    }
}
