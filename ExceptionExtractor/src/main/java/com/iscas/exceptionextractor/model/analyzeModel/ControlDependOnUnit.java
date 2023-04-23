package com.iscas.exceptionextractor.model.analyzeModel;

import soot.Unit;

/**
 * @Author hanada
 * @Date 2023/4/18 20:06
 * @Version 1.0
 */
public class ControlDependOnUnit {

    Unit unit;
    boolean isSatisfy;

    public ControlDependOnUnit(Unit unit, boolean satisfied) {
        this.unit = unit;
        this.isSatisfy = satisfied;
    }

    @Override
    public String toString() {
        return "ControlDependOnUnit{" +
                "unit=" + unit +
                ", isSatisfy=" + isSatisfy +
                '}';
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public boolean isSatisfy() {
        return isSatisfy;
    }

    public void setSatisfy(boolean satisfy) {
        isSatisfy = satisfy;
    }

}
