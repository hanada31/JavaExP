package com.iscas.exceptionextractor.client.soot;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author hanada
 * @Date 2023/4/18 15:16
 * @Version 1.0
 */
public class CFGTraverse {
    SootMethod sootMethod;
    UnitGraph cfg;

    public CFGTraverse(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
        Body body = sootMethod.retrieveActiveBody();
        cfg = new ExceptionalUnitGraph(body);
    }

    List<List<Unit>> allPaths = new ArrayList<>();
    List<Unit> curPath = new ArrayList<>();
    Set<Unit> visited = new HashSet<>();

    public List<List<Unit>> getAllPaths(){
        return allPaths;
    }

    public void traverseAllPaths(){
        traverse(cfg, cfg.getHeads().iterator().next(), curPath, allPaths, visited);
    }

    public void printAllPaths(){
        for (List<Unit> path : allPaths) {
            System.out.println(path);
        }

    }
    private static void traverse(UnitGraph cfg, Unit u, List<Unit> curPath, List<List<Unit>> allPaths, Set<Unit> visited) {
        curPath.add(u);
        visited.add(u);
        if (cfg.getSuccsOf(u).isEmpty()) {
            allPaths.add(new ArrayList<>(curPath));
        } else {
            for (Unit succ : cfg.getSuccsOf(u)) {
                if (!visited.contains(succ)) {
                    traverse(cfg, succ, curPath, allPaths, visited);
                }
            }
        }
        curPath.remove(curPath.size() - 1);
        visited.remove(u);
    }
}
