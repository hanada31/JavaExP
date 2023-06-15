package com.iscas.JavaExP.model.component;

import com.iscas.JavaExP.model.analyzeModel.AppModel;
import com.iscas.JavaExP.utils.ConstantUtils;

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
