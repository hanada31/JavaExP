package com.iscas.crashtracker.base;

import com.iscas.crashtracker.utils.FileUtils;
import soot.options.Options;

/**
 * config information for current run
 * 
 * @author 79940
 *
 */
public class MyConfig {

	private boolean isJimple = true;
	private String resultFolder;
	private String resultWarpperFolder;
	private String appName;
	private String appPath;
	private String client;
	private String callgraphAlgorithm = "SPARK";
	private int timeLimit;
	private String androidJar;
	private boolean stopFlag = false;

	private boolean isSootAnalyzeFinish;
	private boolean isManifestClientFinish;
	private boolean isCallGraphClientFinish;
	private int src_prec = Options.src_prec_apk;
	private int fileSuffixLength = 4;

	private String CrashInfoFilePath;
	private String ExceptionFilePath;
	private String ExceptionFolderPath;
	private String PermissionFilePath;
	private String AndroidCGFilePath ;
	private String AndroidOSVersion = null;

	private String Strategy="";

	private MyConfig() {
	}

	public String getExceptionFolderPath() {
		return ExceptionFolderPath;
	}

	public void setExceptionFolderPath(String exceptionFolderPath) {
		ExceptionFolderPath = exceptionFolderPath;
	}


	private static class SingletonInstance {
		private static final MyConfig INSTANCE = new MyConfig();
	}

	public static MyConfig getInstance() {
		return SingletonInstance.INSTANCE;
	}


	public String getStrategy() {
		return Strategy;
	}

	public void setStrategy(String strategy) {
		Strategy = strategy;
	}
	public boolean isJimple() {
		return isJimple;
	}

	public void setJimple(boolean isJimple) {
		this.isJimple = isJimple;
	}

	public String getResultFolder() {
		return resultFolder;
	}

	public void setResultFolder(String resultFolder) {
		this.resultFolder = resultFolder;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppPath() {
		return appPath;
	}

	public void setAppPath(String appPath) {
		this.appPath = appPath;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public int getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	public void setSrc_prec(int src_prec) {
		this.src_prec = src_prec;
	}
	public int getSrc_prec() {
		return src_prec;
	}

	public String getAndroidOSVersion() {
		return AndroidOSVersion;
	}

	public void setAndroidOSVersion(String androidOSVersion) {
		this.AndroidOSVersion = androidOSVersion;
	}

	/**
	 * @return the androidJar
	 */
	public String getAndroidJar() {
		return androidJar;
	}

	/**
	 * @param androidJar
	 *            the androidJar to set
	 */
	public void setAndroidJar(String androidJar) {
		this.androidJar = androidJar;
	}

	/**
	 * @return the isManifestAnalyzeFinish
	 */
	public boolean isManifestAnalyzeFinish() {
		return isManifestClientFinish;
	}

	/**
	 * @param isManifestAnalyzeFinish
	 *            the isManifestAnalyzeFinish to set
	 */
	public void setManifestAnalyzeFinish(boolean isManifestAnalyzeFinish) {
		this.isManifestClientFinish = isManifestAnalyzeFinish;
	}

	/**
	 * @return the isCallGraphAnalyzeFinish
	 */
	public boolean isCallGraphAnalyzeFinish() {
		return isCallGraphClientFinish;
	}

	/**
	 * @param isCallGraphAnalyzeFinish
	 *            the isCallGraphAnalyzeFinish to set
	 */
	public void setCallGraphAnalyzeFinish(boolean isCallGraphAnalyzeFinish) {
		this.isCallGraphClientFinish = isCallGraphAnalyzeFinish;
	}

	/**
	 * @return the isSootAnalyzeFinish
	 */
	public boolean isSootAnalyzeFinish() {
		return isSootAnalyzeFinish;
	}

	/**
	 * @param isSootAnalyzeFinish
	 *            the isSootAnalyzeFinish to set
	 */
	public void setSootAnalyzeFinish(boolean isSootAnalyzeFinish) {
		this.isSootAnalyzeFinish = isSootAnalyzeFinish;
	}

	/**
	 * @return the stopFlag
	 */
	public boolean isStopFlag() {
		return stopFlag;
	}

	/**
	 * @param stopFlag
	 *            the stopFlag to set
	 */
	public void setStopFlag(boolean stopFlag) {
		this.stopFlag = stopFlag;
	}

	/**
	 * @return the resultWarpperFolder
	 */
	public String getResultWarpperFolder() {
		return resultWarpperFolder;
	}

	/**
	 * @param resultWarpperFolder the resultWarpperFolder to set
	 */
	public void setResultWarpperFolder(String resultWarpperFolder) {
		this.resultWarpperFolder = resultWarpperFolder;
	}


	/**
	 * @return the callgraphAlgorithm
	 */
	public String getCallgraphAlgorithm() {
		return callgraphAlgorithm;
	}

	/**
	 * @param callgraphAlgorithm the callgraphAlgorithm to set
	 */
	public void setCallgraphAlgorithm(String callgraphAlgorithm) {
		this.callgraphAlgorithm = callgraphAlgorithm;
	}

	public int getFileSuffixLength() {
		return fileSuffixLength;
	}

	public void setFileSuffixLength(int fileSuffixLength) {
		this.fileSuffixLength = fileSuffixLength;
	}
	public String getCrashInfoFilePath() {
		return CrashInfoFilePath;
	}

	public void setCrashInfoFilePath(String crashInfoFilePath) {
		CrashInfoFilePath = crashInfoFilePath;
	}

	public String getExceptionFilePath() {
		return ExceptionFilePath;
	}

	public void setExceptionFilePath(String exceptionFilePath) {
		ExceptionFilePath = exceptionFilePath;
		FileUtils.createFolder(ExceptionFilePath);
	}

	public String getAndroidCGFilePath() {
		return AndroidCGFilePath;
	}

	public void setAndroidCGFilePath(String androidCGFilePath) {
		AndroidCGFilePath = androidCGFilePath;
	}


	public String getPermissionFilePath() {
		return PermissionFilePath;
	}

	public void setPermissionFilePath(String permissionFilePath) {
		PermissionFilePath = permissionFilePath;
	}


}