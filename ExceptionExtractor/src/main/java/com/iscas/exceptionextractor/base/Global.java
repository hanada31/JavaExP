package com.iscas.exceptionextractor.base;

import com.iscas.exceptionextractor.model.analyzeModel.AppModel;

public class Global {
	private static final Global instance = new Global();
	private AppModel appModel;
	/**
	 * get the single instance of Global information
	 * include multiple models
	 * @return
	 */
	public static Global v() {
		return instance;
	}

	/**
	 * initialize the Global instance
	 */
	private Global() {
		appModel = new AppModel();
	}

	public AppModel getAppModel() {
		return appModel;
	}

	public void setAppModel(AppModel appModel) {
		this.appModel = appModel;
	}
}
