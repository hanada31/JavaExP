package com.iscas.JavaExP;

import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.client.BaseClient;
import com.iscas.JavaExP.client.cg.cgJava.CallGraphofJavaClient;
import com.iscas.JavaExP.client.exception.ExceptionInfoClient;
import com.iscas.JavaExP.client.soot.IROutputClient;
import com.iscas.JavaExP.utils.TimeUtilsofProject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Main Class of Android ICC Resolution Tool ICCBot
 * 
 * @author hanada
 * @version 2.0 
 */
@Slf4j
public class Main {
	/**
	 * get commands from args
	 * @param args
	 */
	public static void main(String[] args) {
		/** analyze args**/
		CommandLine mCmd = getCommandLine(args);
		analyzeArgs(mCmd);
		/** start**/
		startAnalyze();

		System.exit(0);
	}

	/**
	 * 
	 * @param mCmdArgs
	 * @return
	 */
	private static CommandLine getCommandLine(String[] mCmdArgs) {
		CommandLineParser parser = new DefaultParser();
		try {
			return parser.parse(getOptions(), mCmdArgs, false);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * start the analyze of app with a given client
	 */
	public static void startAnalyze() {
		log.info("Analyzing " + MyConfig.getInstance().getAppName());

		log.info("------Config------");
		log.info(MyConfig.getInstance().toString());
		log.info("------Config------");

		BaseClient client = getClient();

		TimeUtilsofProject.setTotalTimer(client);
		long startTime = System.currentTimeMillis();

		client.start();

		long endTime = System.currentTimeMillis();
		log.info("---------------------------------------");
		log.info(MyConfig.getInstance().getClient() + " time = " + (endTime - startTime) / 1000 + " seconds");
		log.info("Success! Please see files in the result folder "+ MyConfig.getInstance().getResultFolder());
		log.info("Analyzing " + MyConfig.getInstance().getAppName() + " Finish...\n");

	}

	/**
	 * get the client to be analyzed
	 * the default client is used for ICC resolution
	 * @return
	 */
	private static BaseClient getClient() {
		log.info("using client " + MyConfig.getInstance().getClient());
		BaseClient client;
		switch (MyConfig.getInstance().getClient()) {
			case "IROutputClient":
				client = new IROutputClient();
				break;
			case "CallGraphClient":
				client = new CallGraphofJavaClient();
				break;
			case "ExceptionInfoClient":
			default:
				client = new ExceptionInfoClient();
				break;
		}
		return client;
	}
	

	/**
	 * construct the structure of options
	 * 
	 * @return
	 */
	private static Options getOptions() {
		Options options = new Options();

		options.addOption("h", false, "-h: Show the help information.");

		/** input **/
		options.addOption("name", true, "-name: Set the name of the apk under analysis.");
		options.addOption("path", true, "-path: Set the path to the apk under analysis.");
		options.addOption("androidJar", true, "-androidJar: Set the path of android.jar.");
		options.addOption("isJimple", false, "-isJimple: Use Jimple for true, Shimple for false.");

		options.addOption("exceptionPath", true, "-exceptionPath: exception file folder [optional].");
		options.addOption("CGPath", true, "-CGPath: Android CallGraph file [optional.");
		options.addOption("tag", true, "-tag: use that tag to filter unnecessary analysis.");


		/** analysis config **/
		options.addOption("client", true, "-client "
				+ "ExceptionInfoClient: Extract exception information from class or jar files.\n"
				+ "CallGraphClient: Output call graph files.\n"
				+ "ManifestClient: Output manifest.xml file.\n"
				+ "IROutputClient: Output soot IR files.\n"
			);

		/** analysis config **/
		options.addOption("time", true, "-time [default:90]: Set the max running time (min).");
		options.addOption("callgraphAlgorithm", true, "-callgraphAlgorithm [default:SPARK]: Set algorithm for CG, CHA or SPARK.");
		/** output **/
		options.addOption("outputDir", true, "-outputDir: Set the output folder of the apk.");
		options.addOption("sootOutput", false, "-sootOutput: Output the sootOutput");
		options.addOption("exceptionInput", true, "-exceptionInput: exception file folder");
		options.addOption("interProcedure", false, "-interProcedure: true for interProcedure, false for intraProcedure");
		options.addOption("lightWeight", false, "-lightWeight: true for light weight mode, false for normal");
		options.addOption("conflictCheck", false, "-conflictCheck: true for unsat path conflict check, false for not check");

		return options;
	}

	/**
	 * analyze args and store information to MyConfig
	 * @param mCmd 
	 * 
	 */
	private static void analyzeArgs(CommandLine mCmd) {
		if (null == mCmd)
			System.exit(-1);

		if (mCmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(120);
			
			formatter.printHelp("java -jar [jarFile] [options] [-path] [-name] [-outputDir] [-client]", getOptions());
			log.info("E.g., -path apk\\ -name test.apk -outputDir result -client MainClient");
			System.exit(0);
		}

		/** run config **/
		MyConfig.getInstance().setFilterKeyword(mCmd.getOptionValue("filterKeyword", null));
		MyConfig.getInstance().setAppName(mCmd.getOptionValue("name", ""));
		MyConfig.getInstance().setAppPath(mCmd.getOptionValue("path", System.getProperty("user.dir")) + File.separator);
		MyConfig.getInstance().setAndroidJar(mCmd.getOptionValue("androidJar", "lib"+File.separator+"platforms") + File.separator);
		MyConfig.getInstance().setResultFolder(mCmd.getOptionValue("outputDir", "outputDir") + File.separator);
		String resFolder = mCmd.getOptionValue("outputDir", "results"+File.separator+"outputDir")+File.separator;
		if(resFolder.contains(File.separator)){
			resFolder = resFolder.substring(0,resFolder.lastIndexOf(File.separator));
			MyConfig.getInstance().setResultFolder(resFolder+ File.separator);
			MyConfig.getInstance().setExceptionFilePath(MyConfig.getInstance().getResultFolder() +MyConfig.getInstance().getAppName() + File.separator+ "exceptionInfo"+ File.separator);

		}else if(resFolder.contains("//")){
			resFolder = resFolder.substring(0,resFolder.lastIndexOf("//"));
			MyConfig.getInstance().setResultFolder(resFolder+ "//");
			MyConfig.getInstance().setExceptionFilePath(MyConfig.getInstance().getResultFolder() +MyConfig.getInstance().getAppName() + File.separator+ "exceptionInfo"+ File.separator);
		}


		int timeLimit = Integer.valueOf(mCmd.getOptionValue("time", "90"));
		MyConfig.getInstance().setTimeLimit(timeLimit);
		MyConfig.getInstance().setCallgraphAlgorithm(mCmd.getOptionValue("callgraphAlgorithm", "SPARK"));
		if(mCmd.hasOption("interProcedure"))
			MyConfig.getInstance().setInterProcedure(true);
		if(mCmd.hasOption("lightWeight"))
			MyConfig.getInstance().setLightWightMode(true);
		if(mCmd.hasOption("conflictCheck"))
			MyConfig.getInstance().setConflictCheck(true);
		if(mCmd.hasOption("isJimple"))
			MyConfig.getInstance().setJimple(true);
		if(mCmd.hasOption("inConservativeOptimize"))
			MyConfig.getInstance().setConservativeOptimize(false);

		String client = mCmd.getOptionValue("client", "MainClient");
		MyConfig.getInstance().setClient(mCmd.getOptionValue("client", client));

		if (!mCmd.hasOption("name")) {
			printHelp("Please input the apk name use -name.");
		}
	}

	private static void printHelp(String string) {
		log.info(string);
		HelpFormatter formatter = new HelpFormatter();
		log.info("Please check the help inforamtion");
		formatter.printHelp("java -jar CrashTracker.jar [options]", getOptions());
		System.exit(0);
	}

}