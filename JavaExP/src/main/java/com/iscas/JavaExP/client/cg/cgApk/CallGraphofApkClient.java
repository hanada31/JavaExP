package com.iscas.JavaExP.client.cg.cgApk;

import com.iscas.JavaExP.base.Global;
import com.iscas.JavaExP.base.MyConfig;
import com.iscas.JavaExP.client.BaseClient;
import com.iscas.JavaExP.client.cg.CgClientOutput;
import com.iscas.JavaExP.client.cg.CgModify;
import com.iscas.JavaExP.client.manifest.ManifestClient;
import com.iscas.JavaExP.utils.ConstantUtils;
import com.iscas.JavaExP.utils.FileUtils;

import java.io.File;

/**
 * Analyzer Class
 * 
 * @author hanada
 * @version 2.0
 */
public class CallGraphofApkClient extends BaseClient {

	@Override
	protected void clientAnalyze() {
		if (!MyConfig.getInstance().isManifestAnalyzeFinish()) {
			new ManifestClient().start();
			MyConfig.getInstance().setManifestAnalyzeFinish(true);
		}
		CgConstructor cgAnalyzer = new CgConstructor();
		cgAnalyzer.analyze();
		CgModify cgModify = new CgModify();
		cgModify.analyze();
	}

	@Override
	public void clientOutput() {
		/** call graph, if needed, open output**/
		String summary_app_dir = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName()
				+ File.separator;
		FileUtils.createFolder(summary_app_dir + ConstantUtils.CGFOLDETR);
		CgClientOutput.writeCG(summary_app_dir + ConstantUtils.CGFOLDETR,
				Global.v().getAppModel().getAppName()+"_cg.txt", Global.v().getAppModel().getCg());
	}

}