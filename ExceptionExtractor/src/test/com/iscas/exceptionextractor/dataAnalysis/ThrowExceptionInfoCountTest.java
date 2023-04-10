package com.iscas.exceptionextractor.dataAnalysis;

import com.iscas.exceptionextractor.base.MyConfig;

import java.io.File;

/**
 * @Author hanada
 * @Date 2023/4/7 15:23
 * @Version 1.0
 */
class ThrowExceptionInfoCountTest {
    @org.junit.Test
    public void test() {
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        ThrowExceptionInfoCount a = new ThrowExceptionInfoCount();
        a.analyze();
    }
}