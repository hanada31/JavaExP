package com.iscas.exceptionextractor.dataAnalysis;

import com.iscas.exceptionextractor.base.MyConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @Author hanada
 * @Date 2023/4/7 16:47
 * @Version 1.0
 */
@Slf4j
public class ExceptionInfoCountTest {
    @org.junit.Test
    public void testConfig() {
        setArgs();
        log.info("ExceptionInfoCountTest start...");
        setArgs();
        ExceptionInfoCount a = new ExceptionInfoCount();
        a.analyze();
        log.info("ExceptionInfoCountTest Finish...");
        System.exit(0);
    }
//    D:\SoftwareData\dataset\android-framework

    private void setArgs() {
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
    }
}
