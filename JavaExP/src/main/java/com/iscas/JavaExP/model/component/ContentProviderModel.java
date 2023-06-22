package com.iscas.JavaExP.model.component;

import com.iscas.JavaExP.model.analyzeModel.AppModel;
import com.iscas.JavaExP.utils.ConstantUtils;

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