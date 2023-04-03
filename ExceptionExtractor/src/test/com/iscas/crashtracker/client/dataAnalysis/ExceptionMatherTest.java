package com.iscas.crashtracker.client.dataAnalysis;

import com.iscas.crashtracker.base.MyConfig;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:03
 * @Version 1.0
 */
public class ExceptionMatherTest {
    @org.junit.Test
    public void testExceptionMatch() {
        MyConfig.getInstance().setResultFolder("..\\results"  + File.separator);
        MyConfig.getInstance().setExceptionFolderPath("..\\Files"  + File.separator);
        MyConfig.getInstance().setCrashInfoFilePath("..\\Files\\crashInfo.json");
        ExceptionMather matcher = new ExceptionMather();
        matcher.analyze();
    }
}