package com.iscas.crashtracker.utils;

import heros.solver.Pair;
import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.model.analyzeModel.StaticFiledInfo;
import com.iscas.crashtracker.model.sootAnalysisModel.Context;
import com.iscas.crashtracker.model.sootAnalysisModel.Counter;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.shimple.ShimpleBody;
import soot.shimple.internal.SPhiExpr;
import soot.shimple.toolkits.scalar.ShimpleLocalDefs;
import soot.shimple.toolkits.scalar.ShimpleLocalUses;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.io.File;
import java.util.*;

/**
 * type judgment method judgment get information
 * 
 * @author 79940
 *
 */
public class SootUtils {

	/**
	 * get the real name of the component of current dummyMain method instead of
	 * dummyMainClass
	 * 
	 * @param sootMtd
	 * @return
	 */
	public static SootClass getRealClassofDummy(SootMethod sootMtd) {
		SootClass cls;
		Type type = sootMtd.getReturnType();
		if (!(type instanceof RefType))
			return null;
		cls = ((RefType) type).getSootClass();
		return cls;
	}

	/**
	 * judge for each field in class and methods
	 * 
	 * @param sc
	 */
	public static boolean isDialogFragmentClass(SootClass sc) {
		SootClass superCls = sc;
		while (true) {
			if (SootUtils.isOriginDialogFragmentClass(superCls)) {
				return true;
			}
			if (!superCls.hasSuperclass())
				return false;
			superCls = superCls.getSuperclass();
		}
	}

	/**
	 * get Type of ClassName
	 * 
	 * @param sc
	 * @return
	 */
	public static String getNameofClass(SootClass sc) {
		return getNameofClass(sc.getName());
	}

	/**
	 * get Type of ClassName
	 * 
	 * @param sc
	 * @return lamda: dev.ukanth.ufirewall.preferences.-
	 *         $$Lambda$ExpPreferenceFragment$ZOS3OXrmCOVpoNyVtmIXEyWQLi0
	 *         anonymous:
	 *         dev.ukanth.ufirewall.preferences.ExpPreferenceFragment$1
	 */
	public static String getNameofClass(String sc) {
		if (Global.v().getAppModel().getComponentMap().containsKey(sc))
			return sc;
		String className = sc.replace("-$$Lambda$", "").split("\\$")[0];
		if (className.length() == 0)
			className = sc.replace("-", "_").replace("$", "_");
		return className;
	}

	/**
	 * get Type of ClassName
	 * 
	 * @param className
	 * @return
	 */
	public static String getTypeofClassName(String className) {
		Set<String> activitySet = Global.v().getAppModel().getActivityMap().keySet();
		Set<String> serviceSet = Global.v().getAppModel().getServiceMap().keySet();
		Set<String> providerSet = Global.v().getAppModel().getProviderMap().keySet();
		Set<String> receiverSet = Global.v().getAppModel().getRecieverMap().keySet();
		Set<String> fragmentSet = Global.v().getAppModel().getFragmentClasses();

		if (fragmentSet.contains(className)) {
			return "fragment";
		}
		if (activitySet.contains(className)) {
			return "activity";
		}
		if (serviceSet.contains(className)) {
			return "service";
		}
		if (providerSet.contains(className)) {
			return "provider";
		}
		if (receiverSet.contains(className)) {
			return "receiver";
		}
		return "other";
	}

	/**
	 * get Type of ClassName
	 * 
	 * @param sc
	 * @return
	 */
	public static String getTypeofClassName(SootClass sc) {
		String className = getNameofClass(sc);
		return getTypeofClassName(className);
	}

	/**
	 * judge for each field in class and methods
	 * 
	 * @param sc
	 */
	public static boolean isFragmentClass(SootClass sc) {
		if(sc==null) return false;
		if (Global.v().getAppModel().getComponentMap().containsKey(sc.getName()))
			return false;
		SootClass superCls = sc;
		while (true) {
			if (SootUtils.isOriginFragmentClass(superCls)) {
				return true;
			}
			if (!superCls.hasSuperclass())
				return false;
			superCls = superCls.getSuperclass();
		}
	}

	private static boolean isOriginDialogFragmentClass(SootClass sc) {
		if (sc == null)
			return false;
		for (int i = 0; i < ConstantUtils.dialogFragmentClasses.length; i++) {
			if (sc.getName().equals(ConstantUtils.dialogFragmentClasses[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * isFragmentClass used in PotentialRelationAnalyzer
	 * 
	 * @param sc
	 * @return
	 */
	public static boolean isOriginFragmentClass(SootClass sc) {
		if (sc == null)
			return false;
		for (int i = 0; i < ConstantUtils.fragmentClasses.length; i++) {
			if (sc.getName().equals(ConstantUtils.fragmentClasses[i])) {

				return true;
			}
		}
		return false;
	}

	/**
	 * isComponentClass used in PotentialRelationAnalyzer
	 * 
	 * @param sc
	 * @return
	 */
	public static boolean isComponentClass(SootClass sc) {
		for (int i = 0; i < ConstantUtils.componentClasses.length; i++) {
			if (sc.getName().contains(ConstantUtils.componentClasses[i]))
				return true;
		}
		return false;
	}

	/**
	 * judege a method is entry callback or not
	 *
	 * @param method
	 * @return
	 */
	public static boolean isComponentEntryMethod(SootMethod method) {

		if (method.getName().startsWith(ConstantUtils.DUMMYMAIN))
			return true;
		return Global.v().getAppModel().getStubs().contains(method.getSubSignature());
	}

	/**
	 * judege a method is entry callback or not
	 * 
	 * @param method
	 * @return
	 */
	public static boolean isStubEntryMethod(SootMethod method) {
		return method.getDeclaringClass().getName().contains("$Stub$Proxy");
	}

	/**
	 * judge whether active body exist or not if not, retrieve it
	 * 
	 * @param sm
	 * @return
	 */
	public static boolean hasSootActiveBody(SootMethod sm) {
		ArrayList<String> excludeList = new ArrayList<String>();
		addExcludeList(excludeList);

		boolean ready = false;
		if (sm.hasActiveBody()) {
			ready = true;
		} else {
			if (!SootUtils.isNonLibClass(sm.getDeclaringClass().getName())) {
				for (String exPrex : excludeList) {
					if (sm.getSignature().startsWith(exPrex))
						return false;
				}
			}
			try {
				sm.retrieveActiveBody();
				ready = true;
			} catch (RuntimeException e) {
			}
		}
		return ready;
	}

	private static void addExcludeList(ArrayList<String> excludeList) {
		excludeList.add("<android.");
		excludeList.add("<androidx.");
		excludeList.add("<kotlin.");
		excludeList.add("<com.google.");
		excludeList.add("<soot.");
		excludeList.add("<junit.");
		excludeList.add("<java.");
		excludeList.add("<javax.");
		excludeList.add("<sun.");
		excludeList.add("<org.apache.");
		excludeList.add("<org.eclipse.");
		excludeList.add("<org.junit.");
		excludeList.add("<com.fasterxml.");
	}

	/**
	 * judge isLifeCycleMethods
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isLifeCycleMethods(String str) {
		for (int i = 0; i < ConstantUtils.lifeCycleMethodsSet.size(); i++) {
			if (str.contains(ConstantUtils.lifeCycleMethodsSet.get(i)))
				return true;
		}
		return false;
	}

	/**
	 * judge isLifeCycleMethods
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isCallBackMethodShort(String str) {
		final String[] lifeCycleMethods = { "onCreate", "onStart", "onResume", "onPause", "onStop", "onRestart",
				"onDestroy", "onStartCommand", "onBind", "onUnbind", "onRebind", "onReceive" };
		List<String> lifeCycleMethodsSet = Arrays.asList(lifeCycleMethods);
		for (int i = 0; i < lifeCycleMethodsSet.size(); i++) {
			if (str.equals(lifeCycleMethodsSet.get(i)))
				return true;
		}
		return false;
	}

	/**
	 * judge isLifeCycleMethods
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isCallBackMethods(String str) {
		// final String[] lifeCycleMethods = {"onCreate", "onStart", "onResume",
		// "onPause","onStop","onRestart","onDestroy","onStartCommand",
		// "onBind", "onUnbind", "onRebind","onReceive"};
		// List<String> lifeCycleMethodsSet = Arrays.asList(lifeCycleMethods);
		// for (int i = 0; i < lifeCycleMethodsSet.size(); i++) {
		// if (str.equals(lifeCycleMethodsSet.get(i)))
		// return true;
		// }
		// || str.equals("<init>")
		return str.startsWith("on");
	}

	/**
	 * judge isLifeCycleMethods
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isSelfEntryMethods(String str) {
		for (int i = 0; i < ConstantUtils.selfEntryMethodsSet.size(); i++) {
			if (str.contains(ConstantUtils.selfEntryMethodsSet.get(i)))
				return true;
		}
		return false;
	}

	/**
	 * judge isLifeCycleMethods
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isBroadCastRegisterMethods(String str) {
		return str
				.contains(
						"android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)");
	}

	/**
	 * judge is new Intent stmt
	 * 
	 * @param s
	 * @return
	 */
	public static Boolean isNewIntent(Unit s) {
		return s.toString().endsWith("new android.content.Intent");
	}

	/**
	 * judge type is_bundle_extra
	 * 
	 * @param s
	 * @return
	 */
	public static Boolean isBundleExtra(String s) {
		return s.contains("Bundle");
	}

	/**
	 * judge type is_extras_extra
	 * 
	 * @param s
	 * @return
	 */
	public static Boolean isExtrasExtra(String s) {
		return s.contains("Extras");
	}

	/**
	 * judge type isIntentExtra
	 * 
	 * @param s
	 * @return
	 */
	public static Boolean isIntentExtra(String s) {
		return s.contains("Intent");
	}

	/**
	 * judge type isStringType
	 * 
	 * @param extra_type
	 * @return
	 */
	public static boolean isStringType(String extra_type) {
		String[] no = { "Bundle", "Parcelable", "Serializable", "Extras", "ArrayList", "Array" };
		for (String s : no)
			if (extra_type.contains(s))
				return false;
		return true;
	}

	/**
	 * judge type isArrayListType
	 * 
	 * @param extra_type
	 * @return
	 */
	public static boolean isArrayListType(String extra_type) {
		String[] no = { "IntegerArrayList", "ParcelableArrayList", "StringArrayList" };
		for (String s : no)
			if (extra_type.contains(s))
				return true;
		return false;
	}

	/**
	 * judge is_parOrSer_extra type
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isParOrSerExtra(String type) {
		return type.contains("Parcelable") || type.contains("Serializable");
	}

	// method judgment

	/**
	 * judge isSafeLibMethod
	 * 
	 * @param methodStr
	 * @return
	 */
	public static int isSafeLibMethod(String methodStr) {
		for (int i = 0; i < ConstantUtils.unsafePrefix.length; i++) {
			if (methodStr.startsWith(ConstantUtils.unsafePrefix[i]))
				return -1;
		}
		for (int i = 0; i < ConstantUtils.safePrefix.length; i++) {
			if (methodStr.startsWith(ConstantUtils.safePrefix[i]))
				return 1;
		}
		return 0;
	}

	/**
	 * judge isExitPoint method
	 * 
	 * @param methodStr
	 * @return
	 */
	public static boolean isExitPoint(String methodStr) {
		for (int i = 0; i < ConstantUtils.exitpoint.length; i++) {
			if (methodStr.contains(ConstantUtils.exitpoint[i]))
				return true;
		}
		return false;
	}

	/**
	 * judge is_implicit_execute method
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isImplicitExecute(String s) {
		return s.contains(" execute(java.lang.Runnable)");
	}

	/**
	 * judge is_implicit_execute_implict method
	 * 
	 * @param caller
	 * @return
	 */
	public static boolean isImplicitExecuteImplict(String caller) {
		for (String s : ConstantUtils.implicitExecutes)
			if (caller.contains(s))
				return true;
		return false;
	}

	/**
	 * judge is_IntraInvoke_method method
	 * 
	 * @param u
	 * @param cls
	 * @return
	 */
	public static boolean isIntraInvokeMethod(Unit u, String cls) {
		String id = "<" + cls + ":";
		return u.toString().contains("invoke") && u.toString().contains(id);
	}

	// get information

	/**
	 * get InvokeExp from useUnit
	 * 
	 * @param useUnit
	 * @return
	 */
	public static InvokeExpr getInvokeExp(Unit useUnit) {
		InvokeExpr invoke = null;
		if (useUnit instanceof JAssignStmt) {
			JAssignStmt jas = (JAssignStmt) useUnit;
			if (jas.containsInvokeExpr()) {
				invoke = jas.getInvokeExpr();
			}
		} else if (useUnit instanceof JInvokeStmt) {
			invoke = ((JInvokeStmt) useUnit).getInvokeExpr();
		} else if (useUnit instanceof JStaticInvokeExpr) {
			invoke = ((JStaticInvokeExpr) useUnit);
		}
		return invoke;
	}

	/**
	 * getSingleInvokedMethod
	 * 
	 * @param u
	 */
	public static InvokeExpr getSingleInvokedMethod(Unit u) {
		InvokeExpr invoke = null;
		if (u instanceof JAssignStmt) {
			JAssignStmt jas = (JAssignStmt) u;
			if (jas.containsInvokeExpr()) {
				invoke = jas.getInvokeExpr();
			}
		} else if (u instanceof JInvokeStmt) {
			invoke = ((JInvokeStmt) u).getInvokeExpr();
		} else if (u instanceof JStaticInvokeExpr) {
			invoke = ((JStaticInvokeExpr) u);
		}
		return invoke;

	}

	/**
	 * getInvokedMethod
	 * 
	 * @param u
	 * @return
	 */
	public static Set<SootMethod> getInvokedMethodSet(SootMethod sm, Unit u) {
		InvokeExpr invoke = getSingleInvokedMethod(u);
		if (invoke != null) { // u is invoke stmt
			if (Global.v().getAppModel().getUnit2TargetsMap().containsKey(u.toString() + u.hashCode())) {
				return Global.v().getAppModel().getUnit2TargetsMap().get(u.toString() + u.hashCode());
			}
			return addInvokedMethods(sm, u, invoke);
		}
		return new HashSet<SootMethod>();
	}

	/**
	 * getActiveBody
	 * 
	 * @param sm
	 * @return
	 */
	@SuppressWarnings("finally")
	public static Body getSootActiveBody(SootMethod sm) {
		if (sm == null)
			return null;
		if (sm.hasActiveBody())
			return sm.getActiveBody();
		else {
			if (SootUtils.isNonLibClass(sm.getDeclaringClass().getName())) {
				try {
					return sm.retrieveActiveBody();
				} catch (Exception e) {
				} finally {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * get body of sm, without add it to methodsToBeProcessed
	 * 
	 * @param sm
	 * @return
	 */
	public static List<Body> getBodySetofMethod(SootMethod sm) {
		List<Body> bodys = new ArrayList<Body>();
		if (sm.getName().equals("<init>")) {
			for (SootMethod sm2 : sm.getDeclaringClass().getMethods()) {
				if (hasSootActiveBody(sm2) && Global.v().getAppModel().getEntryMethod2Component().containsKey(sm2)) {
					bodys.add(getSootActiveBody(sm2));
				}
			}
		}

		if (hasSootActiveBody(sm)) {
			bodys.add(getSootActiveBody(sm));
		} else if (sm.isAbstract()) {
			Set<SootClass> subClasses = new HashSet<>();
			if (Scene.v().hasFastHierarchy()) {
				if (sm.getDeclaringClass().isInterface()) {
					subClasses = Scene.v().getFastHierarchy().getAllImplementersOfInterface(sm.getDeclaringClass());
				} else {
					subClasses = (Set<SootClass>) Scene.v().getFastHierarchy().getSubclassesOf(sm.getDeclaringClass());
				}
			}
			for (SootClass sc : subClasses) {
				try {
					SootMethod sm2 = sc.getMethodByName(sm.getName());
					if (sm2 != null && hasSootActiveBody(sm2)) {
						bodys.add(getSootActiveBody(sm2));
					}
				} catch (Exception e) {
				}
			}
		}
		return bodys;
	}

	public static SootMethod getMethodBySubSignature(SootClass sootClass, String subSignature) {
		if (sootClass == null) {
			return null;
		}
		SootMethod resultMethod = null;
		for (SootMethod sootMethod : sootClass.getMethods()) {
			if (sootMethod.getSubSignature().contains(subSignature)
					&& sootMethod.getDeclaration().contains("transient")
					&& !sootMethod.getDeclaration().contains("volatile")) {
				return sootMethod;
			}
			if (sootMethod.getSubSignature().contains(subSignature)
					&& sootMethod.getDeclaration().contains("transient")) {
				resultMethod = sootMethod;
				continue;
			}
			if (sootMethod.getSubSignature().contains(subSignature) && resultMethod == null) {
				resultMethod = sootMethod;
			}
		}

		if (resultMethod == null && sootClass.hasSuperclass()) {
			SootClass superclass = sootClass.getSuperclass();
			return getMethodBySubSignature(superclass, subSignature);
		}

		return resultMethod;
	}

	/**
	 * addInvokedMethods in cg construction
	 * 
	 * @param sm
	 * @param u
	 * @param invoke
	 * @return
	 */
	public static Set<SootMethod> addInvokedMethods(SootMethod sm, Unit u, InvokeExpr invoke) {
		Set<SootMethod> targetSet = new HashSet<SootMethod>();
		SootMethod invMethod = invoke.getMethod();
		if (invMethod != null) {
			targetSet.add(invMethod);
			if (invMethod.toString().contains("<java.lang.Thread: void start()>")) {
				SootMethod runMethod = transformStart2Run(sm, invoke, u);
				if (runMethod != null)
					targetSet.add(runMethod);
			} else if (invMethod.toString().contains("java.lang.Thread: void <init>()")) {
				SootMethod runMethod = transformInit2Run(sm, invoke, u);
				if (runMethod != null)
					targetSet.add(runMethod);
			}else if (invMethod.toString().contains("void runOnUiThread(java.lang.Runnable)")) {
				SootMethod runMethod = transformStart2runOnUiThread(sm, invoke, u);
				if (runMethod != null)
					targetSet.add(runMethod);
			} else if (invMethod.toString().contains(
					"android.os.Handler: boolean postDelayed(java.lang.Runnable,long)")) {
				SootMethod runMethod = transformpostDelayed2Run(sm, invoke, u);
				if (runMethod != null)
					targetSet.add(runMethod);
			} else if (invMethod.toString().contains("android.os.AsyncTask execute(java.lang.Object[])")
					|| invMethod.toString().contains("android.os.AsyncTask executeOnExecutor(")) {
				Set<SootMethod> runMethods = transformAsyncTask2LifeCycles(sm, invoke, u);
				for (SootMethod runMethod : runMethods)
					targetSet.add(runMethod);
			} else {
				// get the actual class of the com.iscas.crashtracker.base box in this invocation
				if (invoke instanceof AbstractInstanceInvokeExpr) {
					Value base = ((AbstractInstanceInvokeExpr) invoke).getBase();
					List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(), base, u);
					for (Unit defUnit : defs) {
						String targetCls = ((AbstractDefinitionStmt) defUnit).getRightOp().getType().toString();
						if (targetCls != null && targetCls.length() > 0) {
							SootClass targetClass = SootUtils.getSootClassByName(targetCls);
							if (targetClass == null)
								continue;
							SootMethod targetMtd = SootUtils.getMethodBySubSignature(targetClass,
									invMethod.getSubSignature());
							if (targetMtd == null)
								continue;
							targetSet.add(targetMtd);
							break;
						}
					}
				}
//					targetSet.add(invMethod);
			}
			Pair<SootMethod, Unit> pair = new Pair<SootMethod, Unit>(sm, u);
			Set<SootMethod> listenerMehods = Global.v().getAppModel().getEntryMethod2MethodAddThisCallBack()
					.get(pair);
			if (listenerMehods != null) {
				targetSet.addAll(listenerMehods);
			}
			targetSet.remove(sm);
		} else {
			targetSet.add(invMethod);
		}

		Global.v().getAppModel().getUnit2TargetsMap().put(u.toString() + u.hashCode(), targetSet);
		return targetSet;
	}


	public static boolean isClassInSystemPackage(String className) {
		return className.startsWith("android.") || className.startsWith("java.") || className.startsWith("javax.")
				|| className.startsWith("sun.") || className.startsWith("org.omg.")
				|| className.startsWith("org.w3c.dom.") || className.startsWith("com.google.")
				|| className.startsWith("com.android.") || className.startsWith("com.ibm.")
				|| className.startsWith("com.sun.") || className.startsWith("com.apple.")
				|| className.startsWith("org.w3c.") || className.startsWith("soot");
	}

	public static Set<SootClass> getSootClassesInvoked(SootClass sootClass, Set<SootClass> visitiedClasses,
			Set<SootMethod> visitiedSootMethods) {
		Set<SootClass> sootClassesInvoked = new HashSet<>();

		if (visitiedClasses == null) {
			visitiedClasses = new HashSet<>();
		}
		if (visitiedClasses.contains(sootClass) || isClassInSystemPackage(sootClass.getName())) {
			return sootClassesInvoked;
		}
		sootClassesInvoked.add(sootClass);
		visitiedClasses.add(sootClass);
		List<SootMethod> sootMethods = sootClass.getMethods();
		for (int i = 0; i < sootMethods.size(); i++) {
			SootMethod sootMethod = sootMethods.get(i);
			if (visitiedSootMethods == null) {
				visitiedSootMethods = new HashSet<>();
			}
			if (visitiedSootMethods.contains(sootMethod)) {
				continue;
			}
			visitiedSootMethods.add(sootMethod);
			for (Unit unit : getUnitListFromMethod(sootMethod)) {
				if (unit instanceof Stmt) {
					Stmt stmt = (Stmt) unit;
					if (stmt.containsInvokeExpr()) {
						if (stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName().startsWith("java."))
							continue;
						InvokeExpr invokeExpr = stmt.getInvokeExpr();
						SootMethod invokeMethod = invokeExpr.getMethod();
						SootClass invokeClass = invokeMethod.getDeclaringClass();
						sootClassesInvoked.add(invokeClass);
						sootClassesInvoked.addAll(getSootClassesInvoked(invokeClass, visitiedClasses,
								visitiedSootMethods));
					} else if (unit instanceof JAssignStmt) {
						JAssignStmt assignStmt = (JAssignStmt) unit;
						if (assignStmt.getRightOp() == null) {
							continue;
						}
						Type type = assignStmt.getRightOp().getType();
						if (type instanceof RefType) {
							RefType refType = (RefType) type;
							SootClass refClass = refType.getSootClass();
							if (refClass.getName().startsWith("java."))
								continue;
							sootClassesInvoked.add(refClass);
							sootClassesInvoked.addAll(getSootClassesInvoked(refClass, visitiedClasses,
									visitiedSootMethods));
						}
					}
				}
			}
		}
		return sootClassesInvoked;
	}

	/**
	 * transformStart2Run add call edge
	 * 
	 * @param invoke
	 * @param u
	 * @return
	 */
	protected static SootMethod transformStart2Run(SootMethod sm, InvokeExpr invoke, Unit u) {
		String runSignature = "";
		List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(), ((AbstractInstanceInvokeExpr) invoke).getBase(), u);
		for (Unit def : defs) {
			String type = SootUtils.getTargetClassOfUnit(sm, def);
			if (type.equals("java.lang.Thread")) {
				List<UnitValueBoxPair> uses = SootUtils.getUseOfLocal(sm.getSignature(), def);
				for (UnitValueBoxPair vb : uses) {
					Unit useUnit = vb.getUnit();
					InvokeExpr inv = SootUtils.getInvokeExp(useUnit);
					if (inv == null)
						continue;
					if (inv.getMethod().getSignature().equals("<java.lang.Thread: void <init>(java.lang.Runnable)>")) {
						ValueObtainer vo = new ValueObtainer(sm.getSignature(), "", new Context(), new Counter());
						Set<String> resSet = new HashSet<>(vo.getValueofVar(inv.getArg(0), useUnit, 0).getValues());
						if (resSet != null && resSet.size() > 0)
							type = new ArrayList<String>(resSet).get(0).replace("new ", "");
					}
				}
			}
			runSignature = "<" + type + ": void run()>";
		}
		if (runSignature.length() > 0) {
			SootMethod runMethod = SootUtils.getSootMethodBySignature(runSignature);
			return runMethod;
		}
		return null;
	}

	private static SootMethod transformInit2Run(SootMethod sm, InvokeExpr invoke, Unit u) {

		if(!(invoke instanceof AbstractInstanceInvokeExpr)) return null;
		Value base = ((AbstractInstanceInvokeExpr) invoke).getBase();
		String runSignature = "";
		List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(), base, u);
		for (Unit def : defs) {
			String type = SootUtils.getTargetClassOfUnit(sm, def);
			runSignature = "<" + type + ": void run()>";
		}
		if (runSignature.length() > 0) {
			SootMethod runMethod = SootUtils.getSootMethodBySignature(runSignature);
			return runMethod;
		}
		return null;
	}


	/**
	 * transformpostDelayed2Eun add call edge
	 * 
	 * @param invoke
	 * @param u
	 * @return
	 */
	protected static SootMethod transformpostDelayed2Run(SootMethod sm, InvokeExpr invoke, Unit u) {
		String runSignature = "";
		Value val = null;
		if(invoke instanceof AbstractInvokeExpr){
			val = invoke.getArg(0);
			List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(),val , u);
			for (Unit def : defs) {
				String type = SootUtils.getTargetClassOfUnit(sm, def);
				runSignature = "<" + type + ": void run()>";
			}
			if (runSignature.length() > 0) {
				SootMethod runMethod = SootUtils.getSootMethodBySignature(runSignature);
				return runMethod;
			}
		}
		return null;
	}

	/**
	 * transformStart2Run add call edge
	 * 
	 * @param invoke
	 * @param u
	 * @return
	 */
	protected static SootMethod transformStart2runOnUiThread(SootMethod sm, InvokeExpr invoke, Unit u) {
		String runSignature = "";
		Value v = null;
		if (invoke instanceof JVirtualInvokeExpr)
			v = invoke.getArg(0);
		else if (invoke instanceof JSpecialInvokeExpr)
			v = invoke.getArg(0);
		else
			return null;
		List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(), v, u);
		for (Unit def : defs) {
			String type = SootUtils.getTargetClassOfUnit(sm, def);
			runSignature = "<" + type + ": void run()>";
		}
		if (runSignature.length() > 0) {
			SootMethod runMethod = SootUtils.getSootMethodBySignature(runSignature);
			return runMethod;
		}
		return null;
	}

	/**
	 * transformAsyncTask2LifeCycles
	 * 
	 * @param sm
	 * @param invoke
	 * @param u
	 */
	private static Set<SootMethod> transformAsyncTask2LifeCycles(SootMethod sm, InvokeExpr invoke, Unit u) {
		Set<SootMethod> res = new HashSet<SootMethod>();
		Set<String> candidateMethods = new HashSet<String>();
		if (!(invoke instanceof AbstractInstanceInvokeExpr))
			return res;
		Value val = ((AbstractInstanceInvokeExpr) invoke).getBase();
		List<Unit> defs = SootUtils.getDefOfLocal(sm.getSignature(), val, u);
		for (Unit def : defs) {
			String type = SootUtils.getTargetClassOfUnit(sm, def);
			candidateMethods.add("<" + type + ": java.lang.Object doInBackground(java.lang.Object[])>");
			candidateMethods.add("<" + type + ": void onPostExecute(java.lang.Object)>");
			candidateMethods.add("<" + type + ": void onPreExecute()>");

			candidateMethods.add("<" + type + ": void cancel(boolean)>");
			candidateMethods.add("<" + type + ": void onCancelled(java.lang.Object)>");
			candidateMethods.add("<" + type + ": void (java.lang.Object[])>");
			candidateMethods.add("<" + type + ": void publishProgress()>");
		}
		for (String runSignature : candidateMethods) {
			if (runSignature.length() > 0) {
				SootMethod runMethod = SootUtils.getSootMethodBySignature(runSignature);
				if (runMethod != null) {
					res.add(runMethod);
				}
			}
		}
		return res;
	}

	/**
	 * get Soot Method By Signature
	 * 
	 * @param signature
	 * @return
	 */
	public static SootMethod getSootMethodBySignature(String signature) {
		SootMethod sm = null;
		if (signature == null || signature.length() == 0)
			return null;
		try {
			sm = Scene.v().getMethod(signature);
			if (sm != null)
				return sm;
		} catch (Exception e) {
		}
		return sm;

	}

	/**
	 * get SootClass By Signature
	 * 
	 * @param signature
	 * @return
	 */
	public static SootClass getSootClassByName(String signature) {
		if (signature == null || signature.length() == 0)
			return null;
		try {
			for (SootClass sc : Scene.v().getApplicationClasses()) {
				if (sc.getName().equals(signature))
					return sc;
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * get Return List
	 * 
	 * @param sm
	 * @return
	 */
	public static List<Unit> getRetList(SootMethod sm) {
		List<Unit> rets = new ArrayList<>();
		if (hasSootActiveBody(sm)) {
			rets = new ArrayList<Unit>();
			for (Unit ret_u : getUnitListFromMethod(sm)) {
				if (ret_u instanceof JReturnStmt) {
					rets.add(ret_u);
				}
			}
		}
		return rets;
	}

	/**
	 * get Def Of Local
	 * 
	 * @param u
	 * @return
	 */
	public static List<Unit> getDefOfLocal(String methodName, Value val, Unit u) {
		List<Unit> res = new ArrayList<Unit>();
		if (!(val instanceof Local))
			return res;

		Pair<Value, Unit> pair = new Pair<Value, Unit>(val, u);
		if (Global.v().getAppModel().getUnit2defMap().containsKey(pair))
			return Global.v().getAppModel().getUnit2defMap().get(pair);

		Local local = (Local) val;
		SootMethod sm = null;
		try {
			sm = SootUtils.getSootMethodBySignature(methodName);
		} catch (Exception e) {
			return res;
		}
		if (sm == null)
			return res;
		Body b = getSootActiveBody(sm);
		if (b == null)
			return res;
		UnitGraph graph = new BriefUnitGraph(b);
		if (MyConfig.getInstance().isJimple()) {
			try {
				SimpleLocalDefs defs = new SimpleLocalDefs(graph);
				res = defs.getDefsOfAt(local, u);
			} catch (Exception e) {
				res = new ArrayList<Unit>();
			}
		} else {
			try {
				ShimpleLocalDefs defs = new ShimpleLocalDefs((ShimpleBody) getSootActiveBody(sm));
				res = defs.getDefsOfAt(local, u);
			} catch (Exception e) {
				res = new ArrayList<Unit>();
			}
		}
		Global.v().getAppModel().getUnit2defMap().put(pair, res);
		return res;

	}

	/**
	 * get Use Of Local
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<UnitValueBoxPair> getUseOfLocal(String methodName, Unit defUnit) {
		List<UnitValueBoxPair> res = new ArrayList<UnitValueBoxPair>();
		if (Global.v().getAppModel().getDef2UseMap().containsKey(defUnit))
			return Global.v().getAppModel().getDef2UseMap().get(defUnit);

		SootMethod sm = SootUtils.getSootMethodBySignature(methodName);
		Body b = getSootActiveBody(sm);
		if (b == null)
			return res;
		UnitGraph graph = new BriefUnitGraph(b);
		if (MyConfig.getInstance().isJimple()) {
			try {
				SimpleLocalUses uses = new SimpleLocalUses(graph, new SimpleLocalDefs(graph));
				res = uses.getUsesOf(defUnit);
			} catch (Exception e) {
				res = new ArrayList<UnitValueBoxPair>();
			}
		} else {
			try {
				ShimpleLocalUses uses = new ShimpleLocalUses((ShimpleBody) getSootActiveBody(sm));
				res = uses.getUsesOf(defUnit);
			} catch (Exception e) {
				res = new ArrayList<UnitValueBoxPair>();
			}
		}
		Global.v().getAppModel().getDef2UseMap().put(defUnit, res);
		return res;
	}


	/**
	 * getTargetClassOfUnit used in broadcast type analyze
	 * 
	 * @param defUnit
	 * @return
	 */
	public static String getTargetClassListenerBelongto(Unit defUnit) {
		String className = "";
		if (defUnit instanceof JAssignStmt) {
			JAssignStmt assignDefUnit = (JAssignStmt) defUnit;
			Value rValue = assignDefUnit.getRightOp();
			if (rValue instanceof JInstanceFieldRef) {
				JInstanceFieldRef fieldRef = (JInstanceFieldRef) rValue;
				if (fieldRef.getBase().getType() instanceof PrimType)
					className = fieldRef.getField().getType().toString();
				else {
					RefType type = (RefType) fieldRef.getBase().getType();
					SootClass instanceClass = type.getSootClass();
					className = instanceClass.getName();// + "@" +
														// fieldRef.getField().getName();
				}
			} else if (rValue instanceof JNewExpr) {
				JNewExpr newExpr = (JNewExpr) rValue;
				className = newExpr.getType().toString();
			} else if (rValue instanceof StaticFieldRef) { // static
				StaticFieldRef sfr = (StaticFieldRef) rValue;
				className = sfr.getField().getDeclaringClass().getName();
			} else if (rValue instanceof JInstanceFieldRef) { // static
				JInstanceFieldRef jif = (JInstanceFieldRef) rValue;
				className = jif.getField().getDeclaringClass().getName();
			} else if (rValue instanceof JVirtualInvokeExpr) {
				JVirtualInvokeExpr expr = (JVirtualInvokeExpr) rValue;
				className = expr.getBase().getType().toString();
			} else if (rValue instanceof JStaticInvokeExpr) {
				JStaticInvokeExpr expr = (JStaticInvokeExpr) rValue;
				className = expr.getMethod().getReturnType().toString();
			} else if (rValue instanceof InvokeExpr) {
				InvokeExpr expr = (InvokeExpr) rValue;
				className = expr.getMethod().getReturnType().toString();
			} else if (rValue instanceof SPhiExpr) {
				SPhiExpr expr = (SPhiExpr) rValue;
				className = expr.getType().toString();
				// for (ValueUnitPair arg : expr.getArgs()) {
				// className = arg.getValue().getType().toString();
				// }
			} else if (rValue instanceof JimpleLocal) {
				JimpleLocal expr = (JimpleLocal) rValue;
				className = expr.getType().toString();
			} else if (rValue instanceof JCastExpr) {
				JCastExpr expr = (JCastExpr) rValue;
				className = expr.getCastType().toString();
			} else {
			}
		} else if (defUnit instanceof JIdentityStmt) {
			JIdentityStmt identifyDefUnit = (JIdentityStmt) defUnit;
			Value rValue = identifyDefUnit.getRightOp();
			if (rValue instanceof ThisRef) {
				ThisRef thisRef = (ThisRef) rValue;
				className = thisRef.getType().toString();
			} else if (rValue instanceof ParameterRef) {
				// from parameter, which is unkonwn
				className = null;
			}
		}
		return className;
	}

	/**
	 * getTargetClassOfUnit used in broadcast type analyze
	 * 
	 * @param sm
	 * @param defUnit
	 * @return
	 */
	public static String getTargetClassOfUnit(SootMethod sm, Unit defUnit) {
		String className = "";
		if (defUnit instanceof JAssignStmt) {
			JAssignStmt assignDefUnit = (JAssignStmt) defUnit;
			Value rValue = assignDefUnit.getRightOp();
			if (rValue instanceof JInstanceFieldRef) {
				JInstanceFieldRef fieldRef = (JInstanceFieldRef) rValue;
				if (fieldRef.getField().getType() instanceof PrimType)
					className = fieldRef.getField().getType().toString();
				else {
					SootField field = fieldRef.getField();
					Set<StaticFiledInfo> infos = Global.v().getAppModel().getStaticRefSignature2UnitMap()
							.get(field.getSignature());
					if (infos != null) {
						for (StaticFiledInfo info : infos) {
							if (info.getSootMethod().equals(sm)) {
								className = getTargetClassOfUnit(sm, info.getUnit());
								break;
							}
						}
					}
					if (className.length() == 0) {
						RefType type = (RefType) fieldRef.getField().getType();
						// Object defs =
						// SootUtils.getDefOfLocal(fieldRef.getField(),
						// defUnit);
						SootClass instanceClass = type.getSootClass();
						className = instanceClass.getName();// + "@" +
															// fieldRef.getField().getName();
					}
				}
			} else if (rValue instanceof JNewExpr) {
				JNewExpr newExpr = (JNewExpr) rValue;
				className = newExpr.getType().toString();
			} else if (rValue instanceof StaticFieldRef) { // static
				StaticFieldRef sfr = (StaticFieldRef) rValue;
				className = sfr.getField().getDeclaringClass().getName();
			} else if (rValue instanceof JInstanceFieldRef) { // static
				JInstanceFieldRef jif = (JInstanceFieldRef) rValue;
				className = jif.getField().getDeclaringClass().getName();
			} else if (rValue instanceof JVirtualInvokeExpr) {
				JVirtualInvokeExpr expr = (JVirtualInvokeExpr) rValue;
				className = expr.getMethod().getReturnType().toString();
			} else if (rValue instanceof JStaticInvokeExpr) {
				JStaticInvokeExpr expr = (JStaticInvokeExpr) rValue;
				className = expr.getMethod().getReturnType().toString();
			} else if (rValue instanceof InvokeExpr) {
				InvokeExpr expr = (InvokeExpr) rValue;
				className = expr.getMethod().getReturnType().toString();
			} else if (rValue instanceof SPhiExpr) {
				SPhiExpr expr = (SPhiExpr) rValue;
				className = expr.getType().toString();
				// for (ValueUnitPair arg : expr.getArgs()) {
				// className = arg.getValue().getType().toString();
				// }
			} else if (rValue instanceof JimpleLocal) {
				JimpleLocal expr = (JimpleLocal) rValue;
				className = expr.getType().toString();
			} else if (rValue instanceof JCastExpr) {
				JCastExpr expr = (JCastExpr) rValue;
				className = expr.getCastType().toString();
			} else {
			}
		} else if (defUnit instanceof JIdentityStmt) {
			JIdentityStmt identifyDefUnit = (JIdentityStmt) defUnit;
			Value rValue = identifyDefUnit.getRightOp();
			if (rValue instanceof ThisRef) {
				ThisRef thisRef = (ThisRef) rValue;
				className = thisRef.getType().toString();
			} else if (rValue instanceof ParameterRef) {
				ParameterRef pr = (ParameterRef) rValue;
				className = pr.getType().toString();
			}
		}
		return className;
	}

	public static boolean isMethodReturnUnit(Unit u) {
		return u instanceof JReturnStmt || u instanceof JRetStmt || u instanceof JReturnVoidStmt;
	}

	public static int getIdForUnit(String statement, String method) {
		SootMethod sm = SootUtils.getSootMethodBySignature(method);
		int id = 0;
		for (Unit currentUnit : getUnitListFromMethod(sm)) {
			if (currentUnit.toString().equals(statement)) {
				return id;
			}
			++id;
		}

		return -1;
	}

	public static int getIdForUnit(Unit unit, SootMethod sm) {
		int id = 0;
		Body body = getSootActiveBody(sm);
		if (body == null)
			return -1;
		for (Unit currentUnit : getUnitListFromMethod(sm)) {
			if (currentUnit == unit) {
				return id;
			}
			++id;
		}

		return -1;
	}

	/**
	 * isExtendedLibClass
	 * 
	 * @param clsName
	 */
	public static boolean isNonLibClass(String clsName) {
		for (String lib : Global.v().getAppModel().getExtendedPakgs()) {
			if (clsName.startsWith(lib)) {
				return true;
			}
		}
		return false;
	}

	public static Map<String, Set<String>> getCgMap() {
		List<String> cgList = FileUtils.getListFromFile(MyConfig.getInstance().getResultFolder()
				+ Global.v().getAppModel().getAppName() + File.separator + ConstantUtils.CGFOLDETR + ConstantUtils.CG);
		Map<String, Set<String>> cgMap = new HashMap<String, Set<String>>();
		for (String line : cgList) {
			// <dev.ukanth.ufirewall.Api: boolean
			// assertBinaries(android.content.Context,boolean)> ->
			// <dev.ukanth.ufirewall.util.G: void <clinit>()>
			if (!line.contains(" -> "))
				continue;
			String left = line.split(" -> ")[0];
			String leftClass = left.split(" ")[0];
			leftClass = leftClass.substring(1, leftClass.length() - 1);
			String leftmethod = left.split(" ")[2];
			leftmethod = leftmethod.split("\\(")[0];
			String key = leftClass + " " + leftmethod;

			String right = line.split(" -> ")[1];
			String rightClass = right.split(" ")[0];
			rightClass = rightClass.substring(1, rightClass.length() - 1);
			String rightmethod = right.split(" ")[2].split("\\(")[0];
			String val = rightClass + " " + rightmethod;

			if (!cgMap.containsKey(key))
				cgMap.put(key, new HashSet<String>());
			cgMap.get(key).add(val);
		}
		return cgMap;
	}

	public static Map<String, Set<String>> getCgMapWithSameName() {
		List<String> cgList = FileUtils.getListFromFile(MyConfig.getInstance().getResultFolder()
				+ Global.v().getAppModel().getAppName() + File.separator + ConstantUtils.CGFOLDETR + ConstantUtils.CG);
		Map<String, Set<String>> cgMap = new HashMap<String, Set<String>>();
		for (String line : cgList) {
			// <dev.ukanth.ufirewall.Api: boolean
			// assertBinaries(android.content.Context,boolean)> ->
			// <dev.ukanth.ufirewall.util.G: void <clinit>()>
			if (!line.contains(" -> "))
				continue;
			String left = line.split(" -> ")[0];
			String leftClass = left.split(" ")[0];
			leftClass = leftClass.substring(1, leftClass.length() - 1);
			leftClass = SootUtils.getNameofClass(leftClass);
			String classTypeL = SootUtils.getTypeofClassName(leftClass);
			if (classTypeL.equals("other"))
				continue;
			if (classTypeL.equals("fragment"))
				continue;
			if (classTypeL.equals("provider"))
				continue;

			String leftmethod = left.split(" ")[2];
			leftmethod = leftmethod.split("\\(")[0];
			String key = leftClass + " " + leftmethod;
			String right = line.split(" -> ")[1];
			String rightClass = right.split(" ")[0];
			rightClass = rightClass.substring(1, rightClass.length() - 1);
			rightClass = SootUtils.getNameofClass(rightClass);
			String classTypeR = SootUtils.getTypeofClassName(leftClass);
			if (classTypeR.equals("other"))
				continue;
			if (classTypeR.equals("fragment"))
				continue;
			if (classTypeR.equals("provider"))
				continue;
			String rightmethod = right.split(" ")[2].split("\\(")[0];
			String val = rightClass + " " + rightmethod;

			if (leftmethod.equals(rightmethod)) {
				SootClass leftCls = getSootClassByName(leftClass);
				SootClass rightCls = getSootClassByName(rightClass);
				if (leftCls == null || rightCls == null)
					continue;
				if (leftCls.getSuperclass().getName().equals(rightCls.getName())) {
					if (!cgMap.containsKey(key))
						cgMap.put(key, new HashSet<String>());
					cgMap.get(key).add(val);
				}
			}
		}
		return cgMap;
	}

	/**
	 * get units from a method
	 * soot has StackOverflowError bug for lookup unit
	 * @param m
	 * @return
	 */
	public static List<Unit> getUnitListFromMethod(SootMethod m) {
		List<Unit> units = new ArrayList<Unit>();
		if (m == null || SootUtils.hasSootActiveBody(m) == false)
			return units;
		Iterator<Unit> it = SootUtils.getSootActiveBody(m).getUnits().iterator();
		while (it.hasNext()) {
			Unit u = null;
			try{
				u = it.next();
				if(u instanceof JLookupSwitchStmt){
					u.toString(); //drop it for a bug in soot.jar
				}
			}catch(StackOverflowError e){
//				e.printStackTrace();
				continue;
			}
			units.add(u);
		}
		return units;
	}

	public static void getAllPredsofUnit(SootMethod sootMethod, Unit unit, List<Unit> res) {
		BriefUnitGraph unitGraph = new BriefUnitGraph(sootMethod.getActiveBody());
		List<Unit> predsOf = unitGraph.getPredsOf(unit);
		for (Unit predUnit : predsOf) {
			if(!res.contains(predUnit)) {
				res.add(predUnit);
				getAllPredsofUnit(sootMethod, predUnit, res);
			}
		}
	}

	public static void getAllSuccsofUnit(SootMethod sootMethod, Unit unit, List<Unit> res) {
		BriefUnitGraph unitGraph = new BriefUnitGraph(sootMethod.getActiveBody());
		List<Unit> succesOf = unitGraph.getPredsOf(unit);
		for (Unit succ : succesOf) {
			if(!res.contains(succ)) {
				res.add(succ);
				getAllSuccsofUnit(sootMethod, succ, res);
			}

		}
	}

	/**
	 * getMethodSimpleNameFromSignature
	 * @param str
	 * @return
	 */
	public static String getMethodSimpleNameFromSignature(String str) {
		//<android.database.sqlite.SQLiteOpenHelper: android.database.sqlite.SQLiteDatabase getDatabaseLocked(boolean)>
		String[] ss = str.split(" ");
		String res1 = ss[0].replace("<","").replace(":","");
		String res2 = ss[2].split("\\(")[0];
		return res1 + "." + res2;
	}
	/**
	 * getSootMethodBySimpleName
	 * @param simpleName
	 * @return
	 */
	public static Set<SootMethod> getSootMethodBySimpleName(String simpleName){
		Set<SootMethod> methods = new HashSet<SootMethod>();
		for(SootClass sc: Scene.v().getApplicationClasses()) {
			for(SootMethod method: sc.getMethods()){
				String name = method.getDeclaringClass().getName()+"."+ method.getName();
				if(name.equals(simpleName)){
					methods.add(method);
				}
			}
		}
		return methods;
	}

	public static List<SootClass> getSubClasses(SootMethod sm) {
		List<SootClass> subClasses = new ArrayList<>();
		if (!sm.isAbstract() && !sm.getDeclaringClass().isInterface()) {
			subClasses = Scene.v().getActiveHierarchy().getSubclassesOf(sm.getDeclaringClass());
		}
		return subClasses;
	}

	public static List<SootClass> getSubclassesWithoutMethod(SootClass sc, SootMethod sm) {
		List<SootClass> subClasses = new ArrayList<>();
		if (sm.isAbstract() || sm.getDeclaringClass().isInterface()) return subClasses;
		for (SootClass sub : Scene.v().getActiveHierarchy().getDirectSubclassesOf(sc)) {
			String signature = sm.getSignature().replace(sm.getDeclaringClass().getName(), sub.getName());
			SootMethod subMethod = SootUtils.getSootMethodBySignature(signature);
			if (subMethod == null) {
				subClasses.add(sub);
				subClasses.addAll(getSubclassesWithoutMethod(sub, sm));
			}
		}
		return subClasses;
	}

		public static List<SootClass> getSuperClassesWithAbstract(SootMethod sm) {
		List<SootClass> superClasses = new ArrayList<>();
		if (!sm.isAbstract() && !sm.getDeclaringClass().isInterface()) {
			for (SootClass superCls : Scene.v().getActiveHierarchy().getSuperclassesOf(sm.getDeclaringClass())) {
				String signature = sm.getSignature().replace(sm.getDeclaringClass().getName(), superCls.getName());
				SootMethod superMethod = SootUtils.getSootMethodBySignature(signature);
				if (superMethod != null && superMethod.isAbstract()) {
					superClasses.add(superCls);
				}
			}
		}
		return superClasses;
	}

	public static Set<Integer> getIndexesFromMethod(Edge edge, Set<Integer> paramIndexCallee) {
		SootMethod caller = edge.getSrc().method();
		SootMethod callee = edge.getTgt().method();
		Set<Integer> paramIndexCaller = new HashSet<>();
		if(!caller.hasActiveBody()) return paramIndexCaller;
		for(Unit unit: caller.getActiveBody().getUnits()){
			InvokeExpr invoke = SootUtils.getInvokeExp(unit);
			if(invoke!=null && invoke.getMethod() == callee){
				for(int index: paramIndexCallee){
					Value value = invoke.getArg(index);
					getIndexesFromUnit(new ArrayList<>(),caller, unit, value, paramIndexCaller);
				}
			}
		}

		return paramIndexCaller;
	}

	public static void getIndexesFromUnit(List<Value> valueHistory, SootMethod caller, Unit unit, Value value, Set<Integer> paramIndexCaller) {
		if(valueHistory.contains(value) ) return;  // if defUnit is not a pred of unit
		valueHistory.add(value);
		if(!(value instanceof  Local)) return;
		for(Unit defUnit: SootUtils.getDefOfLocal(caller.getSignature(),value, unit)) {
			if (defUnit instanceof JIdentityStmt) {
				JIdentityStmt identityStmt = (JIdentityStmt) defUnit;
				identityStmt.getRightOp();
				if (identityStmt.getRightOp() instanceof ParameterRef) {
					//from parameter
					paramIndexCaller.add(((ParameterRef) identityStmt.getRightOp()).getIndex());
				}
			} else if (defUnit instanceof JAssignStmt) {
				Value rightOp = ((JAssignStmt) defUnit).getRightOp();
				if (rightOp instanceof Local) {
					getIndexesFromUnit( valueHistory, caller, defUnit, rightOp, paramIndexCaller);
				} else if (rightOp instanceof Expr) {
					if (rightOp instanceof InvokeExpr) {
						InvokeExpr invokeExpr = SootUtils.getInvokeExp(defUnit);
						for (Value val : invokeExpr.getArgs())
							getIndexesFromUnit( valueHistory, caller, defUnit, val, paramIndexCaller);
						if (rightOp instanceof InstanceInvokeExpr) {
							getIndexesFromUnit( valueHistory, caller, defUnit, ((InstanceInvokeExpr) rightOp).getBase(), paramIndexCaller);
						}
					} else if (rightOp instanceof AbstractInstanceOfExpr || rightOp instanceof AbstractCastExpr
							|| rightOp instanceof AbstractBinopExpr || rightOp instanceof AbstractUnopExpr) {
						for (ValueBox vb : rightOp.getUseBoxes()) {
							getIndexesFromUnit( valueHistory, caller, defUnit, vb.getValue(), paramIndexCaller);
						}
					} else if (rightOp instanceof NewExpr) {
						List<UnitValueBoxPair> usesOfOps = SootUtils.getUseOfLocal(caller.getSignature(), defUnit);
						for (UnitValueBoxPair use : usesOfOps) {
							for (ValueBox vb : use.getUnit().getUseBoxes())
								getIndexesFromUnit( valueHistory, caller, defUnit, vb.getValue(), paramIndexCaller);
						}
					}
				}else if (rightOp instanceof JArrayRef) {
					JArrayRef jArrayRef = (JArrayRef) rightOp;
					getIndexesFromUnit( valueHistory, caller, defUnit, jArrayRef.getBase(), paramIndexCaller);
				}else if (rightOp instanceof JInstanceFieldRef) {
					JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
					getIndexesFromUnit( valueHistory, caller, defUnit, jInstanceFieldRef.getBase(), paramIndexCaller);
				} else {
//                    rvalue = constant | local | expr | array_ref | instance_field_ref |
//                            next_next_stmt_address | static_field_ref;
//                    System.err.println(rightOp.getClass());
				}
			}
		}
	}


	public static boolean fieldIsChanged(SootField field, SootMethod sootMethod) {
		for(Unit u: sootMethod.getActiveBody().getUnits()){
			if(u instanceof  JAssignStmt){
				JAssignStmt jAssignStmt = (JAssignStmt) u;
				if(jAssignStmt.getLeftOp() instanceof  FieldRef){
					if (field ==  jAssignStmt.getFieldRef().getField()) {
						return true;
					}
				}else if(jAssignStmt.getRightOp() instanceof  FieldRef){
					if(field.getType() instanceof  PrimType) continue;
					if (field ==  jAssignStmt.getFieldRef().getField()) {
						List<UnitValueBoxPair> uses = SootUtils.getUseOfLocal(sootMethod.getSignature(), jAssignStmt);
						for(UnitValueBoxPair pair:uses){
							if( pair.getUnit() instanceof JAssignStmt){
								JAssignStmt jAssignStmt2 = (JAssignStmt) pair.getUnit();
								if(jAssignStmt2.getRightOp() != pair.getValueBox().getValue()){
									return true;
								}
							}else if( pair.getUnit() instanceof JInvokeStmt){
								SootMethod met = ((JInvokeStmt) pair.getUnit()).getInvokeExpr().getMethod();
								if(!met.getName().startsWith("get"))
									return true;
							}
						}
					}
				}
			}
		}
		return false;
	}


	public static List<Value> getFiledValueAssigns(Value base, SootField f, List<Unit> allPreds) {
		List<Value> rightValues = new ArrayList<>();
		String name = f.getName();
		for (Unit predUnit : allPreds) {
			if(predUnit instanceof  AssignStmt){
				Value left =  ((AssignStmt) predUnit).getLeftOp();
				if(left instanceof AbstractInstanceFieldRef){
					if(((AbstractInstanceFieldRef) left).getField().getName().equals(name)){
						if(((AbstractInstanceFieldRef) left).getBase() == base) {
							for (ValueBox vb : ((AssignStmt) predUnit).getRightOp().getUseBoxes())
								rightValues.add(vb.getValue());
						}
					}
				}
			}
		}
		return rightValues;
	}

	public static boolean isHardwardRelated(String name) {
		for (int i = 0; i < ConstantUtils.hardwares.length; i++) {
			if (name.contains(ConstantUtils.hardwares[i])) {
				return true;
			}
		}
		return false;
	}
}
