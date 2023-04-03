package com.iscas.crashtracker.model.analyzeModel;

import heros.solver.Pair;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.crash.CrashInfo;
import com.iscas.crashtracker.model.component.ComponentModel;
import com.iscas.crashtracker.utils.ConstantUtils;
import com.iscas.crashtracker.utils.PrintUtils;
import com.iscas.crashtracker.utils.SootUtils;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.io.Serializable;
import java.util.*;

public class AppModel implements Serializable {
	private static final long serialVersionUID = 1L;
	// app info
	private final String appName;
	private final String appPath;
	private String mainActivity;
	private String manifestString;
	private String packageName;
	private int versionCode;
	private String permission;
	private Set<String> usesPermissionSet;
	private Set<String> applicationClassNames;


	// call graph related
	private final Set<SootMethod> allMethods;
	private Set<SootMethod> entryMethods;
	private CallGraph cg = new CallGraph();

	private final List<SootMethod> topoMethodQueue;
	private Set<List<SootMethod>> topoMethodQueueSet;

	// components
	private final HashMap<String, ComponentModel> componentMap;
	private final HashMap<String, ComponentModel> activityMap;
	private final HashMap<String, ComponentModel> serviceMap;
	private final HashMap<String, ComponentModel> providerMap;
	private final HashMap<String, ComponentModel> receiverMap;
	private final HashMap<String, ComponentModel> exportedComponentMap;
	
	private Set<String> FragmentClasses;
	private Set<String> callbacks;
	private Set<String> stubs;
	private Set<String> extendedPkgss;

	// info collect

	private final Map<String, String> StaticRefSignature2initAssignMap; // for static
	private Map<String, Set<StaticFiledInfo>> StaticRefSignature2UnitMap; // for
	// static
	private Map<String, Set<SootMethod>> unit2TargetsMap;
	private Map<Unit, List<UnitValueBoxPair>> def2UseMap;
	private Map<Pair<Value, Unit>, List<Unit>> unit2defMap;
	private Map<SootMethod, SootClass> entryMethod2Component;
	private Map<Pair<SootMethod, Unit>, Set<SootMethod>> entryMethod2MethodAddThisCallBack;
	private Map<Unit, List<ParameterSource>> unit2ParameterSource;
	private List<CrashInfo> crashInfoList;

	
	public AppModel() {
		String name = MyConfig.getInstance().getAppName();
		appPath = MyConfig.getInstance().getAppPath() + name;
		if(MyConfig.getInstance().getAppName().endsWith(".apk") || MyConfig.getInstance().getAppName().endsWith(".jar")) {
			MyConfig.getInstance().setFileSuffixLength(4);
		}else {
			MyConfig.getInstance().setFileSuffixLength(0);
		}
		appName = name.substring(0, name.length() - MyConfig.getInstance().getFileSuffixLength());
		manifestString = "";
		packageName = "";
		permission = "";
		mainActivity = "";

		allMethods = new HashSet<>();
		entryMethods = new HashSet<>();
		topoMethodQueue = new ArrayList<SootMethod>();
		topoMethodQueueSet = new HashSet<List<SootMethod>>();

		activityMap = new HashMap<String, ComponentModel>();
		serviceMap = new HashMap<String, ComponentModel>();
		providerMap = new HashMap<String, ComponentModel>();
		receiverMap = new HashMap<String, ComponentModel>();
		componentMap = new HashMap<String, ComponentModel>();
		exportedComponentMap = new HashMap<String, ComponentModel>();
		FragmentClasses = new HashSet<String>();
		applicationClassNames = new HashSet<String>();

		usesPermissionSet = new HashSet<String>();
		StaticRefSignature2initAssignMap = new HashMap<String, String>();
		StaticRefSignature2UnitMap = new HashMap<String, Set<StaticFiledInfo>>();

		unit2TargetsMap = new HashMap<String, Set<SootMethod>>();

		setDef2UseMap(new HashMap<Unit, List<UnitValueBoxPair>>());
		setUnit2defMap(new HashMap<Pair<Value, Unit>, List<Unit>>());
		callbacks = new HashSet<>();
		stubs = new HashSet<>();
		entryMethod2Component = new HashMap<SootMethod, SootClass>();
		entryMethod2MethodAddThisCallBack = new HashMap<Pair<SootMethod, Unit>, Set<SootMethod>>();
		unit2ParameterSource = new HashMap<Unit, List<ParameterSource>>();
		setExtendedPakgs(new HashSet<String>());
		crashInfoList = new ArrayList<>();
	}

	@Override
	public String toString() {
		String res = "";
		res += "appName: " + appName + "\n";
		res += "appPath: " + appPath + "\n";
		res += "packageName: " + packageName + "\n";
		res += "permission: " + permission + "\n";
		res += "usesPermissionSet: " + PrintUtils.printSet(usesPermissionSet) + "\n";
		res += "activityMap: " + PrintUtils.printMap(activityMap) + "\n";
		res += "serviceMap: " + PrintUtils.printMap(serviceMap) + "\n";
		res += "providerMap: " + PrintUtils.printMap(providerMap) + "\n";
		res += "receiverMap: " + PrintUtils.printMap(receiverMap) + "\n";
		res += "eaMap: " + PrintUtils.printMap(exportedComponentMap) + "\n";

		return res;
	}

	public List<CrashInfo> getCrashInfoList() {
		return crashInfoList;
	}

	public String getAppPath() {
		return appPath;
	}

	public void setUsesPermissionSet(Set<String> usesPermissionSet) {
		this.usesPermissionSet = usesPermissionSet;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getManifestString() {
		return manifestString;
	}

	public void setManifestString(String manifestString) {
		this.manifestString = manifestString;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public HashMap<String, ComponentModel> getActivityMap() {
		return activityMap;
	}

	public HashMap<String, ComponentModel> getServiceMap() {
		return serviceMap;
	}

	public HashMap<String, ComponentModel> getProviderMap() {
		return providerMap;
	}

	public HashMap<String, ComponentModel> getRecieverMap() {
		return receiverMap;
	}

	public HashMap<String, ComponentModel> setComponentMap(HashMap<String, ComponentModel> map) {
		componentMap.putAll(map);
		return componentMap;
	}

	public HashMap<String, ComponentModel> getComponentMap() {
		return componentMap;
	}

	public HashMap<String, ComponentModel> getExportedComponentMap() {
		return exportedComponentMap;
	}

	public String getAppName() {
		return appName;
	}

	public Map<String, String> getStaticRefSignature2initAssignMap() {
		return StaticRefSignature2initAssignMap;
	}


	public List<SootMethod> getTopoMethodQueue() {
		return topoMethodQueue;
	}

	public CallGraph getCg() {
		return cg;
	}

	public void setCg(CallGraph callGraph) {
		this.cg = callGraph;

	}

	public String getMainActivity() {
		return mainActivity;
	}

	public void setMainActivity(String mainActivity) {
		this.mainActivity = mainActivity;
	}


	public Set<String> getFragmentClasses() {
		return FragmentClasses;
	}

	public Map<String, Set<SootMethod>> getUnit2TargetsMap() {
		return unit2TargetsMap;
	}

	public Set<List<SootMethod>> getTopoMethodQueueSet() {
		return topoMethodQueueSet;
	}



	/**
	 * @return the versionCode
	 */
	public int getVersionCode() {
		return versionCode;
	}

	/**
	 * @param versionCode
	 *            the versionCode to set
	 */
	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	/**
	 * @return the def2UseMap
	 */
	public Map<Unit, List<UnitValueBoxPair>> getDef2UseMap() {
		return def2UseMap;
	}

	/**
	 * @param def2UseMap
	 *            the def2UseMap to set
	 */
	public void setDef2UseMap(Map<Unit, List<UnitValueBoxPair>> def2UseMap) {
		this.def2UseMap = def2UseMap;
	}

	/**
	 * @return the unit2defMap
	 */
	public Map<Pair<Value, Unit>, List<Unit>> getUnit2defMap() {
		return unit2defMap;
	}

	/**
	 * @param unit2defMap
	 *            the unit2defMap to set
	 */
	public void setUnit2defMap(Map<Pair<Value, Unit>, List<Unit>> unit2defMap) {
		this.unit2defMap = unit2defMap;
	}

	/**
	 * @return the callbacks
	 */
	public Set<String> getCallbacks() {
		return callbacks;
	}

	/**
	 * @param callbacks
	 *            the callbacks to set
	 */
	public void setCallbacks(Set<String> callbacks) {
		this.callbacks = callbacks;
	}

	/**
	 * @return the staticRefSignature2UnitMap
	 */
	public Map<String, Set<StaticFiledInfo>> getStaticRefSignature2UnitMap() {
		return StaticRefSignature2UnitMap;
	}

	/**
	 * @return the callBacks2Component
	 */
	public Map<SootMethod, SootClass> getEntryMethod2Component() {
		return entryMethod2Component;
	}

	public void addEntryMethod2Component(SootMethod sm, SootClass sc) {
		if (sm.getDeclaringClass().getName().contains(".R$"))
			return;
		if (sm.getDeclaringClass().getName().contains(ConstantUtils.DUMMYMAIN))
			sc = SootUtils.getRealClassofDummy(sm);
		if (sc == null)
			return;
		this.entryMethod2Component.put(sm, sc);
	}


	/**
	 * @return the stubs
	 */
	public Set<String> getStubs() {
		return stubs;
	}

	/**
	 * @return the entryMethod2MethodAddThisCallBack
	 */
	public Map<Pair<SootMethod, Unit>, Set<SootMethod>> getEntryMethod2MethodAddThisCallBack() {
		return entryMethod2MethodAddThisCallBack;
	}



	public void addUnit2ParameterSource(Unit unit, ParameterSource ps) {
		if (!unit2ParameterSource.containsKey(unit))
			unit2ParameterSource.put(unit, new ArrayList<ParameterSource>());
		for (ParameterSource exist : unit2ParameterSource.get(unit)) {
			if (exist.toString().equals(ps.toString()))
				return;
		}
		unit2ParameterSource.get(unit).add(ps);
	}

	/**
	 * @return the applicationClassNames
	 */
	public Set<String> getApplicationClassNames() {
		return applicationClassNames;
	}

	/**
	 * @param sc
	 * the applicationClasses to set
	 */
	public void addApplicationClassNames(String sc) {
		this.applicationClassNames.add(sc);
	}

	/**
	 * @return the extendedLibs
	 */
	public Set<String> getExtendedPakgs() {
		return extendedPkgss;
	}

	/**
	 * @param extendedLibs
	 *            the extendedLibs to set
	 */
	public void setExtendedPakgs(Set<String> extendedLibs) {
		this.extendedPkgss = extendedLibs;
	}

	/**
	 * @return the entryMethods
	 */
	public Set<SootMethod> getEntryMethods() {
		return entryMethods;
	}



}
