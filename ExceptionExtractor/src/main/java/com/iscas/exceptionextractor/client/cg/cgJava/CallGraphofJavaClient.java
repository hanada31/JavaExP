package com.iscas.exceptionextractor.client.cg.cgJava;

import com.iscas.exceptionextractor.base.Global;
import com.iscas.exceptionextractor.client.BaseClient;
import lombok.extern.slf4j.Slf4j;
import soot.jimple.toolkits.callgraph.CallGraph;
import com.iscas.exceptionextractor.base.MyConfig;
import com.iscas.exceptionextractor.client.cg.CgClientOutput;
import com.iscas.exceptionextractor.utils.ConstantUtils;
import com.iscas.exceptionextractor.utils.FileUtils;

import java.io.File;

/**
 * Analyzer Class
 * 
 * @author hanada
 * @version 2.0
 */
@Slf4j
public class CallGraphofJavaClient extends BaseClient {

	@Override
	protected void clientAnalyze() {
		log.info("Start analyze with CallGraphClient.");
		CallGraph cg = CallGraphBuilder.getCallGraph();
		Global.v().getAppModel().setCg(cg);
		log.info("Call Graph has " + cg.size() + " edges.");
		log.info("Successfully analyze with CallGraphClient.");

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