package com.iscas.crashtracker.client.manifest;

import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.BaseClient;
import com.iscas.crashtracker.client.soot.SootAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;

import java.io.IOException;

/**
 * Analyzer Class
 * 
 * @author hanada
 * @version 2.0
 */
@Slf4j
public class ManifestClient extends BaseClient {

	@Override
	protected void clientAnalyze() {
		if (!MyConfig.getInstance().isSootAnalyzeFinish()) {
			SootAnalyzer sootAnalyzer = new SootAnalyzer();
			sootAnalyzer.analyze();
		}
		ManifestAnalyzer manifestAnalyzer = new ManifestAnalyzer();
		manifestAnalyzer.analyze();

		log.info("Successfully analyze with ManifestClient.");
	}

	@Override
	public void clientOutput() throws IOException, DocumentException {
//		String summary_app_dir = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName()
//				+ File.separator;
//		FileUtils.createFolder(summary_app_dir + ConstantUtils.MANIFOLDETR);
//
//		/** manifest **/
//		ManifestClientOutput.writeManifest(summary_app_dir + ConstantUtils.MANIFOLDETR, ConstantUtils.MANIFEST);


		// String content = Global.v().getAppModel().getAppName() + "\t" +
		// Global.v().getAppModel().getPackageName()
		// + "\t" + Global.v().getAppModel().getComponentMap().size() + "\t"
		// + Global.v().getAppModel().getExportedComponentMap().size() + "\t"
		// + Global.v().getAppModel().getActivityMap().size() + "\n";
		// FileUtils.writeText2File(MyConfig.getInstance().getResultFolder() +
		// "componentNumber.txt", content, true);

	}

}