package com.iscas.JavaExP.utils;

import java.io.File;

public class ConstantUtils {
	public static final Object VERSION = "1.0";

	public static final String ACTIVITY = "Activity";
	public static final String SERVICE = "Service";
	public static final String RECEIVER = "Receiver";
	public static final String PROVIDER = "Provider";
	// soot config
	public static final String SOOTOUTPUT = "SootIRInfo";

	// output files info
	public static final String CGFOLDETR = "CallGraphInfo" + File.separator;
	public static final String CG = "cg.txt";
	public static final String DUMMYMAIN = "dummyMain";

	// Constant number
	public static final int GETVALUELIMIT = 1000;


	public static final String BROADCAST_ONRECEIVE = "void onReceive(android.content.Context,android.content.Intent)";

	public static final String CONTENTPROVIDER_ONCREATE = "boolean onCreate()";

	public static final String onCreateOptionsMenu = "boolean onCreateOptionsMenu(android.view.Menu)";
	public static final String onOptionsItemSelected = "boolean onOptionsItemSelected(android.view.MenuItem)";
    public static final int SIGNLARCALLERDEPTH = 10;
    public static final int CFGPATHNUMBER = 500;
    public static final int ENDUNITMAXNUMBER = 10;
	public static final int CFGPATHNODELEN = 50;
    public static final long SINGLEMETHODTIME = 20* 1000; //20s
    public static final CharSequence REQUIRENOTNULL = "<java.util.Objects: java.lang.Object requireNonNull(";
    public static final String FORMALPARA = "@parameter@";
    public static final int CGCALLCHAINLIMIT = 10;
	public static final int EXCEPTIONINFOSIZE = 100;


	public static String CGANALYSISPREFIX = "android";
    public static String FRAMEWORKPREFIX = "android";
	public static final int CONDITIONHISTORYSIZE = 50;


}
