package com.iscas.crashtracker.client.soot;

import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.BaseClient;
import lombok.extern.slf4j.Slf4j;
import soot.PackManager;

/**
 * Analyzer Class
 * 
 * @author hanada
 * @version 2.0
 */
@Slf4j
public class IROutputClient extends BaseClient {

	@Override
	protected void clientAnalyze() {
		if (!MyConfig.getInstance().isSootAnalyzeFinish()) {
			SootAnalyzer sootAnalyzer = new SootAnalyzer();
			sootAnalyzer.analyze();
		}
		log.info("Successfully analyze with IROutputClient.");
	}

	@Override
	public void clientOutput() {
		PackManager.v().writeOutput();
	}

}