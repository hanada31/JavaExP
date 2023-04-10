package com.iscas.exceptionextractor.base;

import com.iscas.exceptionextractor.model.analyzeModel.AppModel;

public abstract class Analyzer {
	public AppModel appModel;

	public Analyzer() {
		this.appModel = Global.v().getAppModel();
	}

	public abstract void analyze();

}
