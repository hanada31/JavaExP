package com.iscas.JavaExP.utils;

import lombok.extern.slf4j.Slf4j;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

/**
 * @Author hanada
 * @Date 2023/4/18 15:16
 * @Version 1.0
 */
@Slf4j
public class CFGTraverse {
    SootMethod sootMethod;
    UnitGraph cfg;
    Map<Unit, Integer> endUnitNumMap;
    public CFGTraverse(SootMethod sootMethod) {
        this.sootMethod = sootMethod;
        Body body = sootMethod.retrieveActiveBody();
        cfg = new BriefUnitGraph(body);
        endUnitNumMap = new HashMap<>();
    }

    List<List<Unit>> allPaths = new ArrayList<>();
    List<Unit> curPath = new ArrayList<>();
    Set<Unit> visited = new HashSet<>();

    public List<List<Unit>> getAllPaths(){
        return allPaths;
    }

    public void traverseAllPaths(){
        log.info(sootMethod.getSignature() +" start analysis");
        traverse(cfg);
        log.info(sootMethod.getSignature() +" start end");
    }

    public void traverseAllPathsEndWithThrow(){
//        log.info(sootMethod.getSignature() +" start analysis");
        traverseWithThrow(cfg);
//        log.info(sootMethod.getSignature() +" start end");
    }

    public void printAllPaths(){
        for (List<Unit> path : allPaths) {
            System.out.println(path);
        }

    }

    public List<List<Unit>> traverseAllPathsEndWithThrowAndInvoke( long startMS) {
        endUnitNumMap = new HashMap<>();
        Stack<PathNode> stack = new Stack<>(); // 用于存储路径节点的栈
        for (Unit u : cfg.getHeads()) {
            stack.push(new PathNode(u)); // 初始化根节点
        }
        while (!stack.isEmpty()) {
            if(System.currentTimeMillis() - startMS > ConstantUtils.SINGLEMETHODTIME) {
                log.info("Timeout while executing traverseAllPathsEndWithThrowAndInvoke @ " +sootMethod.getSignature() );
                break;
            }
            if(allPaths.size()> ConstantUtils.CFGPATHNUMBER) return allPaths;
            PathNode node = stack.pop();
            Unit u = node.getCurrentUnit();
            InvokeExpr invokeExpr = SootUtils.getInvokeExp(u);
            if (cfg.getSuccsOf(u).isEmpty()) {
                //for the callee
                if(isTargetInvoke(invokeExpr)  || SootUtils.isNotCaughtThrowUnit(sootMethod,u)) { // 仅保存end at throw 的
                    addPathToAllPath(u,node);
                }
            } else {
                if(isTargetInvoke(invokeExpr) ){
                    addPathToAllPath(u,node);
                }
                for (Unit succ : cfg.getSuccsOf(u)) {
                    if(node.getPath().size()>ConstantUtils.CFGPATHNODELEN) break;
                    if(node.getPath().contains(succ)) break;
                    PathNode newNode = new PathNode(succ, node.getPath());
                    stack.push(newNode); // 添加后继节点到栈中
                }
            }
        }
        return allPaths;
    }

    private boolean isTargetInvoke(InvokeExpr invokeExpr) {
        if(invokeExpr !=null && invokeExpr.getMethod().hasActiveBody()){
            if( invokeExpr.getMethod().getSignature().contains(ConstantUtils.REQUIRENOTNULL)) return true;
            if(invokeExpr.getMethod().getDeclaringClass().getName().startsWith("java.") || invokeExpr.getMethod().getDeclaringClass().getName().startsWith("javax.")){
                return false;
            }
            return true;
        }
        return false;
    }

    private void addPathToAllPath(Unit u, PathNode node) {
        if(!endUnitNumMap.containsKey(u) ||endUnitNumMap.get(u)< ConstantUtils.ENDUNITMAXNUMBER) {
            allPaths.add(node.getPath()); // 找到一条路径
            updateEndUnitNumMap(u);
        }
    }

    private List<List<Unit>> traverseWithThrow(UnitGraph cfg) {
        endUnitNumMap = new HashMap<>();
        Stack<PathNode> stack = new Stack<>(); // 用于存储路径节点的栈
        for (Unit u : cfg.getHeads()) {
            stack.push(new PathNode(u)); // 初始化根节点
        }
        while (!stack.isEmpty()) {
            if(allPaths.size()> ConstantUtils.CFGPATHNUMBER) return allPaths;
            PathNode node = stack.pop();
            Unit u = node.getCurrentUnit();

            if (cfg.getSuccsOf(u).isEmpty()) {
                if (SootUtils.isNotCaughtThrowUnit(sootMethod,u)) { // 仅保存end at throw 的
                    addPathToAllPath(u,node);
                }
            } else {
                for (Unit succ : cfg.getSuccsOf(u)) {
                    if(node.getPath().size()>ConstantUtils.CFGPATHNODELEN) break;
                    if(node.getPath().contains(succ)) break;
                    PathNode newNode = new PathNode(succ, node.getPath());
                    stack.push(newNode); // 添加后继节点到栈中
                }
            }
        }
        return allPaths;
    }

    private void updateEndUnitNumMap(Unit u) {
        if(endUnitNumMap.containsKey(u)){
            endUnitNumMap.put(u, endUnitNumMap.get(u)+1);
        }else {
            endUnitNumMap.put(u, 1);
        }
    }

    private List<List<Unit>> traverse(UnitGraph cfg) {
        endUnitNumMap = new HashMap<>();
        Stack<PathNode> stack = new Stack<>(); // 用于存储路径节点的栈

        for (Unit u : cfg.getHeads()) {
            stack.push(new PathNode(u)); // 初始化根节点
        }

        while (!stack.isEmpty()) {
            if(allPaths.size()> ConstantUtils.CFGPATHNUMBER) return allPaths;
            PathNode node = stack.pop();
            Unit u = node.getCurrentUnit();

            if (cfg.getSuccsOf(u).isEmpty()) {
                allPaths.add(node.getPath()); // 找到一条路径
            } else {
                for (Unit succ : cfg.getSuccsOf(u)) {
                    if(node.getPath().size()>ConstantUtils.CFGPATHNODELEN) break;
                    if(node.getPath().contains(succ)) break;
                    PathNode newNode = new PathNode(succ, node.getPath());
                    stack.push(newNode); // 添加后继节点到栈中
                }
            }
        }
        return allPaths;
    }
}

class PathNode {
    private Unit currentUnit; // 当前基本块
    private List<Unit> path; // 已访问的基本块的路径列表

    public PathNode(Unit currentUnit) {
        this(currentUnit, new ArrayList<>());
    }

    public PathNode(Unit currentUnit, List<Unit> path) {
        this.currentUnit = currentUnit;
        this.path = new ArrayList<>(path);
        this.path.add(currentUnit);
    }

    public Unit getCurrentUnit() {
        return currentUnit;
    }

    public List<Unit> getPath() {
        return path;
    }
}

