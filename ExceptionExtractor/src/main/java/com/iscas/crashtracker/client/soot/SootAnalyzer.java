package com.iscas.crashtracker.client.soot;

import com.google.common.collect.Lists;
import com.iscas.crashtracker.base.Analyzer;
import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.utils.ConstantUtils;
import lombok.extern.slf4j.Slf4j;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
@Slf4j
public class SootAnalyzer extends Analyzer {
	public SootAnalyzer() {
		super();
	}

	/**
	 * analyze using soot 1) set setActiveBody 2) def-use analyze
	 */
	@Override
	public void analyze() {
		if (appModel.getAppName() == null)
			return;
		sootInit();
		sootTransform();
		sootEnd();
		if (Global.v().getAppModel().getApplicationClassNames().size() == 0) {
			for (SootClass sc : Scene.v().getApplicationClasses()) {
				Global.v().getAppModel().addApplicationClassNames(sc.getName());
			}
			log.info("Soot Analysis finish.");
		}
	}

	/**
	 * initialize soot
	 */
	public static void sootInit() {
		soot.G.reset();
		if(Global.v().getAppModel().getAppPath().endsWith(".apk") ){
			Options.v().set_android_jars(MyConfig.getInstance().getAndroidJar());
			MyConfig.getInstance().setSrc_prec(Options.src_prec_apk);
		}else{
			MyConfig.getInstance().setSrc_prec(Options.src_prec_only_class);

		}
		List<String> processDir = Lists.newArrayList();
		processDir.add( Global.v().getAppModel().getAppPath());
		Options.v().set_include_all(true);
		Options.v().set_process_dir(processDir);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_process_multiple_dex(true);
		if (MyConfig.getInstance().isJimple())
			Options.v().set_output_format(Options.output_format_jimple);
		else
			Options.v().set_output_format(Options.output_format_shimple);
		String out = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName() + File.separator
				+ ConstantUtils.SOOTOUTPUT;
//		Options.v().set_output_dir(out);
		Options.v().set_src_prec(MyConfig.getInstance().getSrc_prec());
		Options.v().allow_phantom_refs();
		Options.v().set_whole_program(true);
		setExcludePackage();
	}


	/**
	 * add transforms for analyzing
	 */
	private void sootTransform() {
		String pack = "jtp";
		if (!MyConfig.getInstance().isJimple())
			pack = "stp";
		// set setActiveBody
		ActiveBodyTransformer abTran = new ActiveBodyTransformer();
		Transform t1 = new Transform(pack + ".bt", abTran);
		PackManager.v().getPack(pack).add(t1);

	}

	/**
	 * end setting of soot
	 */
	public static void sootEnd() {
		soot.Main.v().autoSetOptions();
		Scene.v().loadNecessaryClasses();
		Scene.v().loadBasicClasses();
		PackManager.v().runPacks();
	}

	/**
	 * packages refuse to be analyzed
	 */
	public static void setExcludePackage() {
		ArrayList<String> excludeList = new ArrayList<String>();
//		excludeList.add("android.*");
//		excludeList.add("androidx.*");
//		excludeList.add("kotlin.*");
		excludeList.add("soot.*");
		excludeList.add("junit.*");
		excludeList.add("java.*");
		excludeList.add("javax.*");
		excludeList.add("sun.*");
		Options.v().set_exclude(excludeList);
	}
}
