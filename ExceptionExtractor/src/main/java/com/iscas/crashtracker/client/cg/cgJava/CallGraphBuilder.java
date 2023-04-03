package com.iscas.crashtracker.client.cg.cgJava;

import com.iscas.crashtracker.utils.ConstantUtils;
import com.iscas.crashtracker.utils.SootUtils;
import lombok.extern.slf4j.Slf4j;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
@Slf4j
public class CallGraphBuilder {

	private static final CallGraph cg = Scene.v().getCallGraph();

	static {
//		filterEdges(cg);
		log.info("filtered CG Edge Size ***** " + cg.size());
		addEdgesByOurAnalyze(cg);
		log.info("enhanced CG Edge Size ***** " + cg.size());
	}

	private static void filterEdges(CallGraph cg) {
		Set<Edge> res = new HashSet<>();
		Iterator<Edge> it = cg.iterator();
		while(it.hasNext()){
			Edge e = it.next();
			if(!e.getSrc().method().getDeclaringClass().getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX) ||
					!e.getTgt().method().getDeclaringClass().getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX)){
				res.add(e);
			}
		}
		for(Edge e: res) {
			cg.removeEdge(e);
		}
	}

	public static CallGraph getCallGraph() {
		return cg;
	}

	private static void addEdgesByOurAnalyze(CallGraph callGraph) {
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if(!sc.getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX)) continue;
			ArrayList<SootMethod> methodList = new ArrayList<SootMethod>(sc.getMethods());
			for (SootMethod sm : methodList) {
				if (SootUtils.hasSootActiveBody(sm) == false)
					continue;
				Iterator<Unit> it = SootUtils.getSootActiveBody(sm).getUnits().iterator();
				while (it.hasNext()) {
					Unit u = it.next();
					InvokeExpr exp = SootUtils.getInvokeExp(u);
					if (exp == null)  continue;
					InvokeExpr invoke = SootUtils.getSingleInvokedMethod(u);
					if (invoke != null) { // u is invoke stmt
						Set<SootMethod> targetSet = SootUtils.getInvokedMethodSet(sm, u);
						for (SootMethod target : targetSet) {
							if(!target.getDeclaringClass().getPackageName().startsWith(ConstantUtils.CGANALYSISPREFIX)) continue;
							Edge e = new Edge(sm, (Stmt) u, target);
							callGraph.addEdge(e);
						}
					}
				}
			}
		}
	}
}

