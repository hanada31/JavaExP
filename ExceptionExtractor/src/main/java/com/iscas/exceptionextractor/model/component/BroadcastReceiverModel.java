package com.iscas.exceptionextractor.model.component;

import com.iscas.exceptionextractor.model.analyzeModel.AppModel;
import com.iscas.exceptionextractor.utils.ConstantUtils;

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
