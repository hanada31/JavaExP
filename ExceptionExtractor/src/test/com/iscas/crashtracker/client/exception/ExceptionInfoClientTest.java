package com.iscas.crashtracker.client.exception;

import com.iscas.crashtracker.Main;
import com.iscas.crashtracker.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:02
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoClientTest {

    @org.junit.Test
    public void testConfig() {
        setArgs();
        Main.startAnalyze();
        log.info("ExceptionInfoClientTest Finish...\n");
        System.exit(0);
    }

    private void setArgs() {
        String path, androidVersion;
        path = "C:\\Users\\yanjw\\programs\\framework\\classes\\";
        path = "D:\\SoftwareData\\dataset\\android-framework\\classes\\";
        MyConfig.getInstance().setAndroidOSVersion("2.3");
//        MyConfig.getInstance().setAndroidOSVersion("");

        String client = "ExceptionInfoClient";

        androidVersion = "android"+MyConfig.getInstance().getAndroidOSVersion();;
        MyConfig.getInstance().setAppName(androidVersion);
        MyConfig.getInstance().setAppPath(path + File.separator);
        MyConfig.getInstance().setClient(client);
        MyConfig.getInstance().setResultWarpperFolder("..\\results" + File.separator);
        MyConfig.getInstance().setResultFolder("..\\Files" + File.separator);
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
