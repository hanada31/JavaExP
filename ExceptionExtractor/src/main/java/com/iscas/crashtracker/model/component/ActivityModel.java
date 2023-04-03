package com.iscas.crashtracker.model.component;

import com.iscas.crashtracker.model.analyzeModel.AppModel;
import com.iscas.crashtracker.utils.ConstantUtils;

import java.io.Serializable;

/**
 * ActivityModel extends ComponentModel
 * 
 * @author 79940
 *
 */
public class ActivityModel extends ComponentModel implements Serializable, Launchable {
	private static final long serialVersionUID = 2L;
	private String launchMode="";
	private String taskAffinity;

	@Override
	public String getComponentType() {
		return ConstantUtils.ACTIVITY;
	}

	public ActivityModel(AppModel appModel) {
		super(appModel);
		type = "a";
	}

	@Override
	public String getLaunchMode() {
		return launchMode;
	}

	public void setLaunchMode(String launchMode) {
		this.launchMode = launchMode;
	}

	public String getTaskAffinity() {
		return taskAffinity;
	}

	public void setTaskAffinity(String taskAffinity) {
		this.taskAffinity = taskAffinity;
	}

	@Override
	public String toString() {
		String res = "launchMode: " + launchMode;
		return super.toString() + res + "\n";
	}
}
