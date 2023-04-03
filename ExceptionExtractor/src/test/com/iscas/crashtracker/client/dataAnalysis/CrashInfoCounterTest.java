package com.iscas.crashtracker.client.dataAnalysis;

import com.iscas.crashtracker.base.MyConfig;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:02
 * @Version 1.0
 */
public class CrashInfoCounterTest {

    @org.junit.Test
    public void testCrashInfoCounter() {
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        MyConfig.getInstance().setCrashInfoFilePath("..\\Files\\crashInfo.json");
        CrashInfoCounter matcher = new CrashInfoCounter();
        matcher.analyze();
    }
}