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
    String[] versions = {"2.1", "4.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0"};

    @org.junit.Test
    public void testConfig() {
        log.info("ExceptionInfoClientTest start...\n");
        setArgs();
        Main.startAnalyze();
        log.info("ExceptionInfoClientTest Finish...\n");
        System.exit(0);
    }

    private void setArgs() {
        String path, fileVersion;
        String client = "ExceptionInfoClient";

        MyConfig.getInstance().setFileVersion("1.8");
        path = "D:\\SoftwareData\\dataset\\android-framework\\jars";
        MyConfig.getInstance().setAppPath(path + File.separator);
        fileVersion = "jdk"+MyConfig.getInstance().getFileVersion();
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
    }
}
