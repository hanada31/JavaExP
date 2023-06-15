package com.iscas.JavaExP.client.cg.cgJava;

import com.iscas.JavaExP.base.Global;
import com.iscas.JavaExP.client.BaseClient;
import com.iscas.JavaExP.client.cg.CgModify;
import lombok.extern.slf4j.Slf4j;
import soot.jimple.toolkits.callgraph.CallGraph;

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
		CgModify cgModify = new CgModify();
		cgModify.analyze();
		Global.v().getAppModel().setCg(cg);
		log.info("Call Graph has " + cg.size() + " edges.");
		log.info("Successfully analyze with CallGraphClient.");

	}

	@Override
	public void clientOutput() {
		/** call graph, if needed, open output**/
//		String summary_app_dir = MyConfig.getInstance().getResultFolder() + Global.v().getAppModel().getAppName()
//				+ File.separator;
//		FileUtils.createFolder(summary_app_dir + ConstantUtils.CGFOLDETR);
//		CgClientOutput.writeCG(summary_app_dir + ConstantUtils.CGFOLDETR,
//				Global.v().getAppModel().getAppName()+"_cg.txt", Global.v().getAppModel().getCg());

	}

}