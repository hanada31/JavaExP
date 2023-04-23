package com.iscas.exceptionextractor.client.exception;

import soot.Unit;

/**
 * @Author hanada
 * @Date 2023/4/18 20:06
 * @Version 1.0
 */
public class DominateUnit {
    Unit unit;
    boolean isSatisfy;

    public DominateUnit(Unit unit, boolean satisfied) {
        this.unit = unit;
        this.isSatisfy = satisfied;
    }

    @Override
    public String toString() {
        return "DominateUnit{" +
                "unit=" + unit +
                ", isSatisfy=" + isSatisfy +
                '}';
    }
}
