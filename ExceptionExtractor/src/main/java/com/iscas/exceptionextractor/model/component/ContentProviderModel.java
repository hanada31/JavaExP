package com.iscas.exceptionextractor.model.component;

import com.iscas.exceptionextractor.model.analyzeModel.AppModel;
import com.iscas.exceptionextractor.utils.ConstantUtils;

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
