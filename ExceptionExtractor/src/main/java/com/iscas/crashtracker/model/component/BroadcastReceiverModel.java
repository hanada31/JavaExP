package com.iscas.crashtracker.model.component;

import com.iscas.crashtracker.model.analyzeModel.AppModel;
import com.iscas.crashtracker.utils.ConstantUtils;

public class BroadcastReceiverModel extends ComponentModel {
	private static final long serialVersionUID = 3L;

	public BroadcastReceiverModel(AppModel appModel) {
		super(appModel);
		type = "r";
	}

	@Override
	public String getComponentType() {
		return ConstantUtils.RECEIVER;
	}

}
