package com.iscas.exceptionextractor.dataAnalysis;

import com.iscas.exceptionextractor.Main;
import com.iscas.exceptionextractor.base.MyConfig;
import lombok.extern.slf4j.Slf4j;
import soot.options.Options;

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
        log.info("ExceptionInfoCountTest start...\n");
        setArgs();
        ExceptionInfoCount a = new ExceptionInfoCount();
        a.analyze();
        log.info("ExceptionInfoCountTest Finish...\n");
        System.exit(0);
    }


    private void setArgs() {
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
    }
}
