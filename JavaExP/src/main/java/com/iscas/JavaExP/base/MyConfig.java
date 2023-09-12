package com.iscas.JavaExP.base;

import com.iscas.JavaExP.utils.FileUtils;
import soot.options.Options;

/**
 * config information for current run
 * 
 * @author 79940
 *
 */
public class MyConfig {

	private boolean isJimple = false;
	private boolean isInterProcedure = false;
	private boolean isConservativeOptimize = true;
	private String resultFolder;
	private String appName;
	private String appPath;
	private String client;
	private String callgraphAlgorithm = "SPARK";
	private int timeLimit;
	private String androidJar;
	private boolean stopFlag = false;
	private String filterKeyword ;
	private boolean writeOutput = false;
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
	private String FileVersion = null;

	private String Strategy="";
	private boolean lightWightMode=false;
	private boolean conflictCheck=false;



	private MyConfig() {
 	}

	public String getExceptionFolderPath() {
		return ExceptionFolderPath;
	}

	public void setExceptionFolderPath(String exceptionFolderPath) {
		ExceptionFolderPath = exceptionFolderPath;
	}

	public boolean isInterProcedure() {
		return isInterProcedure;
	}

	public void setInterProcedure(boolean interProcedure) {
		isInterProcedure = interProcedure;
	}

	public boolean isConservativeOptimize() {
		return isConservativeOptimize;
	}

	public void setConservativeOptimize(boolean conservativeOptimize) {
		isConservativeOptimize = conservativeOptimize;
	}

	public boolean isWriteOutput() {
		return writeOutput;
	}

	public void setWriteOutput(boolean writeOutput) {
		this.writeOutput = writeOutput;
	}

	public boolean isLightWightMode() {
		return lightWightMode;
	}

	public void setLightWightMode(boolean lightWightMode) {
		this.lightWightMode = lightWightMode;
	}

	public boolean isConflictCheck() {
		return conflictCheck;
	}

	public void setConflictCheck(boolean conflictCheck) {
		this.conflictCheck = conflictCheck;
	}


	private static class SingletonInstance {
		private static final MyConfig INSTANCE = new MyConfig();
	}

	public static MyConfig getInstance() {
		return SingletonInstance.INSTANCE;
	}

	public String getFilterKeyword() {
		return filterKeyword;
	}

	public void setFilterKeyword(String filterKeyword) {
		this.filterKeyword = filterKeyword;
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

//	/**
//	 * @return the resultWarpperFolder
//	 */
//	public String getResultWarpperFolder() {
//		return resultWarpperFolder;
//	}
//
//	/**
//	 * @param resultWarpperFolder the resultWarpperFolder to set
//	 */
//	public void setResultWarpperFolder(String resultWarpperFolder) {
//		this.resultWarpperFolder = resultWarpperFolder;
//	}


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


	@Override
	public String toString() {
		return "MyConfig{" +
				"\n lightWightMode=" + lightWightMode +
				"\n isInterProcedure=" + isInterProcedure +
				"\n isConservativeOptimize=" + isConservativeOptimize +
				"\n conflictCheck=" + conflictCheck +
				"\n writeOutput=" + writeOutput +
				"\n filterKeyword='" + filterKeyword + '\'' +
				"\n isJimple=" + isJimple +
				"\n client='" + client + '\'' +
				"\n callgraphAlgorithm='" + callgraphAlgorithm + '\'' +
				"\n appName='" + appName + '\'' +
				"\n appPath='" + appPath + '\'' +
				"\n resultFolder='" + resultFolder + '\'' +
				"\n timeLimit=" + timeLimit +
//				"\n androidJar='" + androidJar + '\'' +
//				"\n stopFlag=" + stopFlag +
//				"\n isSootAnalyzeFinish=" + isSootAnalyzeFinish +
//				"\n isManifestClientFinish=" + isManifestClientFinish +
//				"\n isCallGraphClientFinish=" + isCallGraphClientFinish +
//				"\n src_prec=" + src_prec +
//				"\n fileSuffixLength=" + fileSuffixLength +
//				"\n CrashInfoFilePath='" + CrashInfoFilePath + '\'' +
//				"\n ExceptionFilePath='" + ExceptionFilePath + '\'' +
//				"\n ExceptionFolderPath='" + ExceptionFolderPath + '\'' +
//				"\n PermissionFilePath='" + PermissionFilePath + '\'' +
//				"\n AndroidCGFilePath='" + AndroidCGFilePath + '\'' +
//				"\n FileVersion='" + FileVersion + '\'' +
//				"\n Strategy='" + Strategy + '\'' +
				"\n }";
	}
}