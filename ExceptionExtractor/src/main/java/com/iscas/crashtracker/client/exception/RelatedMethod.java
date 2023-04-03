package com.iscas.crashtracker.client.exception;

import com.iscas.crashtracker.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanada
 * @Date 2022/3/22 17:59
 * @Version 1.0
 */


public class RelatedMethod {

    private String method;
    private RelatedMethodSource source;
    private List<String> trace = new ArrayList<>();
    private int depth;

    public RelatedMethod() {
    }

    public RelatedMethod(String method, RelatedMethodSource source, int depth, List<String> trace) {
        this.method = method;
        this.source = source;
        this.depth = depth;
        this.trace = trace;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public RelatedMethodSource getSource() {
        return source;
    }

    public void setSource(RelatedMethodSource source) {
        this.source = source;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "RelatedMethod{" +
                "method='" + method + '\'' +
                ", source=" + source +
                ", depth=" + depth +
                ",trace=" + PrintUtils.printList(trace) +
                '}';
    }

    public List<String> getTrace() {
        return trace;
    }
    public void setTrace(List<String> trace) {
        this.trace = trace;
    }
    public void addTrace(String t) {
        this.trace.add(t);
    }
}
