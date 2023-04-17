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
        String path, fileVersion;
        String client = "ExceptionInfoClient";

        MyConfig.getInstance().setFileVersion("1.8");
        path = "..\\M_framework\\";
//        path = "D:\\SoftwareData\\dataset\\android-framework\\jars";
        MyConfig.getInstance().setAppPath(path + File.separator);
        fileVersion = "commons-io-2.6";
        MyConfig.getInstance().setAppName(fileVersion+".jar");

        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        MyConfig.getInstance().setTimeLimit(100);
        MyConfig.getInstance().setAndroidJar("E:\\AndroidSDK\\android-sdk-windows-new\\platforms");
        MyConfig.getInstance().setSrc_prec(Options.src_prec_only_class);
        MyConfig.getInstance().setFileSuffixLength(0);
        String androidFolder = MyConfig.getInstance().getResultFolder() +File.separator+fileVersion+File.separator;
        MyConfig.getInstance().setExceptionFilePath(androidFolder+"exceptionInfo"+File.separator);
        MyConfig.getInstance().setPermissionFilePath(androidFolder+"Permission"+File.separator+"permission.txt");
        MyConfig.getInstance().setAndroidCGFilePath(androidFolder+"CallGraphInfo"+File.separator+"cg.txt");
        MyConfig.getInstance().setJimple(false);

    }
}
