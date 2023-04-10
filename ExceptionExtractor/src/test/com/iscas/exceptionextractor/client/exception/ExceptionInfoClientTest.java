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

    @org.junit.Test
    public void testConfig() {
        log.info("ExceptionInfoClientTest start...\n");
        setArgs();
        Main.startAnalyze();
        log.info("ExceptionInfoClientTest Finish...\n");
        System.exit(0);
    }

    private void setArgs() {
        String path, androidVersion;
        path = "..\\M_framework\\";
        MyConfig.getInstance().setAndroidOSVersion("2.3");
        String client = "ExceptionInfoClient";

        androidVersion = "android"+MyConfig.getInstance().getAndroidOSVersion();;
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
    }
}
