package com.iscas.crashtracker.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

	// Android project build
	public static final String DEFAULTCALLBACKFILE = "AndroidCallbacks.txt";

	// Android APIs
	public static final String[] implicitExecutes = { "onPreExecute(", "doInBackground(", "onPostExecute(" };
	public static final String[] implicitStart = { "start(" };

	public static final String ACTIVITY_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONSTART = "void onStart()";
	public static final String ACTIVITY_ONRESTOREINSTANCESTATE = "void onRestoreInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPOSTCREATE = "void onPostCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONRESUME = "void onResume()";
	public static final String ACTIVITY_ONPOSTRESUME = "void onPostResume()";
	public static final String ACTIVITY_ONCREATEDESCRIPTION = "java.lang.CharSequence onCreateDescription()";
	public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPAUSE = "void onPause()";
	public static final String ACTIVITY_ONSTOP = "void onStop()";
	public static final String ACTIVITY_ONRESTART = "void onRestart()";
	public static final String ACTIVITY_ONDESTROY = "void onDestroy()";

	public static final String SERVICE_ONCREATE = "void onCreate()";
	public static final String SERVICE_ONSTART1 = "void onStart(android.content.Intent,int)";
	public static final String SERVICE_ONSTART2 = "int onStartCommand(android.content.Intent,int,int)";
	public static final String SERVICE_ONBIND = "android.os.IBinder onBind(android.content.Intent)";
	public static final String SERVICE_ONREBIND = "void onRebind(android.content.Intent)";
	public static final String SERVICE_ONUNBIND = "boolean onUnbind(android.content.Intent)";
	public static final String SERVICE_ONDESTROY = "void onDestroy()";

	public static final String FRAGMENT_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String FRAGMENT_ONATTACH = "void onAttach(android.app.Activity)";
	public static final String FRAGMENT_ONCREATEVIEW = "android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)";
	public static final String FRAGMENT_ONSTART = "void onStart()";
	public static final String FRAGMENT_ONACTIVITYCREATED = "void onActivityCreated(android.os.Bundle)";
	public static final String FRAGMENT_ONVIEWSTATERESTORED = "void onViewStateRestored(android.app.Activity)";
	public static final String FRAGMENT_ONRESUME = "void onResume()";
	public static final String FRAGMENT_ONPAUSE = "void onPause()";
	public static final String FRAGMENT_ONSTOP = "void onStop()";
	public static final String FRAGMENT_ONDESTROYVIEW = "void onDestroyView()";
	public static final String FRAGMENT_ONDESTROY = "void onDestroy()";
	public static final String FRAGMENT_ONDETACH = "void onDetach()";

	public static final String BROADCAST_ONRECEIVE = "void onReceive(android.content.Context,android.content.Intent)";

	public static final String CONTENTPROVIDER_ONCREATE = "boolean onCreate()";

	public static final String onCreateOptionsMenu = "boolean onCreateOptionsMenu(android.view.Menu)";
	public static final String onOptionsItemSelected = "boolean onOptionsItemSelected(android.view.MenuItem)";
    public static final int LARGECALLERSET = 10 ;
    public static final int SIGNLARCALLERDEPTH = 5;


    public static String CGANALYSISPREFIX = "android";
    public static String FRAMEWORKPREFIX = "android";

	public static final int MANIFESTTIMEOUT =2 ;

	public static final int CONDITIONHISTORYSIZE = 50;
	public static final int CALLDEPTH = 8;
    public static final int EXTENDCGDEPTH = 5;

    public static final int INITSCORE = 100;
	public static final int SECONDINITSCORE = 70;
	public static final int METHODINTACE = 5;
    public static final int OUTOFPKGSCORE = 20;
	public static final int OUTOFTRACESCORE = 20 ;
	public static final int BOTTOMSCORE = 0;
	public static final int DIFFCLASS = 20;
	public static final int SMALLGAPSCORE = 1;
	public static final int LARGEGAPSCORE = 5;


	private static final String[] selfEntryMethods = { onCreateOptionsMenu, onOptionsItemSelected };

	private static final String[] lifeCycleMethods = { ACTIVITY_ONCREATE, ACTIVITY_ONDESTROY, ACTIVITY_ONPAUSE,
			ACTIVITY_ONRESTART, ACTIVITY_ONRESUME, ACTIVITY_ONSTART, ACTIVITY_ONSTOP, ACTIVITY_ONSAVEINSTANCESTATE,
			ACTIVITY_ONRESTOREINSTANCESTATE, ACTIVITY_ONCREATEDESCRIPTION, ACTIVITY_ONPOSTCREATE,
			ACTIVITY_ONPOSTRESUME,

			SERVICE_ONCREATE, SERVICE_ONDESTROY, SERVICE_ONSTART1, SERVICE_ONSTART2, SERVICE_ONBIND, SERVICE_ONREBIND,
			SERVICE_ONUNBIND,

			BROADCAST_ONRECEIVE,

			CONTENTPROVIDER_ONCREATE,

			FRAGMENT_ONCREATE, FRAGMENT_ONDESTROY, FRAGMENT_ONPAUSE, FRAGMENT_ONATTACH, FRAGMENT_ONDESTROYVIEW,
			FRAGMENT_ONRESUME, FRAGMENT_ONSTART, FRAGMENT_ONSTOP, FRAGMENT_ONCREATEVIEW, FRAGMENT_ONACTIVITYCREATED,
			FRAGMENT_ONVIEWSTATERESTORED, FRAGMENT_ONDETACH };
	public static final List<String> lifeCycleMethodsSet = Arrays.asList(lifeCycleMethods);
	public static final List<String> selfEntryMethodsSet = Arrays.asList(selfEntryMethods);

	
	public static final String[] unsafePrefix = {
			"<android.content.Context: java.lang.Object getSystemService(java.lang.String)>(\"activity\")",
			"<android.content.SharedPreferences", "<android.content.ContentProvider", "<android.app.Application",
			"<android.content.ContextWrapper", "<java.io.File", "android.content.ComponentName getCallingActivity()" };

	public static final String[] safePrefix = { "<android.content.Intent",
			"<android.content.Context: java.lang.Object getSystemService(java.lang.String)>" };
	public static final String[]  exitpoint= { "finish()", "throw " };

	public static String[] fragmentClasses = { "android.app.Fragment",
			"com.actionbarsherlock.app.SherlockListFragment", "android.support.v4.app.Fragment",
			"android.support.v4.app.ListFragment", "androidx.fragment.app.Fragment" };

	public static String[] dialogFragmentClasses = { "android.support.v4.app.DialogFragment",
			"androidx.fragment.app.DialogFragment" };

	public static String[] componentClasses = { "android.app.Activity", "android.app.Service",
			"android.content.BroadcastReceiver", "android.content.ContentProvider" };

	public static String[] hardwares = {"hardware", "opengl", "AccountManager", "AndroidHttpClient", "AudioManager", "AudioRecord", "BluetoothAdapter",
			"BluetoothHeadset", "Camera", "Chronometer", "ContentResolver", "DrmManagerClient", "EffectContext", "IsoDep",
			"KeyguardManager.KeyguardLock", "LruCache", "LocationManager", "MediaCodec", "MediaPlayer", "MediaRecorder", "MtpDevice",
			"MifareClassic", "MifareUltralight", "Ndef", "NdefFormatable", "NfcA", "NfcB", "NfcBarcode", "NfcF", "NfcV", "NfcAdapter",
			"NsdManager", "ParcelFileDescriptor", "PowerManager$WakeLock", "PresetReverb", "RemoteCallbackList", "SQLiteClosable",
			"SensorManager", "SlidingDrawer", "StorageManager", "Surface", "SurfaceHolder", "TagTechnology",
			"TokenWatcher", "UsbManager", "VelocityTracker", "WebIconDatabase", "WifiManager$MulticastLock", "WifiManager$WifiLock", "WifiManager"};
	//strategies

}
