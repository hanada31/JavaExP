package com.iscas.crashtracker.model.component;

import com.iscas.crashtracker.model.analyzeModel.AppModel;
import com.iscas.crashtracker.utils.ConstantUtils;

public class ContentProviderModel extends ComponentModel {
	private static final long serialVersionUID = 3L;

	public ContentProviderModel(AppModel appModel) {
		super(appModel);
		type = "p";
	}

	@Override
	public String getComponentType() {
		return ConstantUtils.PROVIDER;
	}
}
