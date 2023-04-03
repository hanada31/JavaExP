package com.iscas.crashtracker.client.dataAnalysis;

import com.iscas.crashtracker.base.MyConfig;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author hanada
 * @Date 2022/9/8 14:01
 * @Version 1.0
 */
public class BuggyStatisticTest {
    @org.junit.Test
    public void test() {
        MyConfig.getInstance().setResultFolder("..\\results" + File.separator);
        BuggyStatistic a = new BuggyStatistic();
        a.analyze();
    }
}