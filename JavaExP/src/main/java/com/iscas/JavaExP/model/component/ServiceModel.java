package com.iscas.JavaExP.model.component;

import com.iscas.JavaExP.model.analyzeModel.AppModel;
import com.iscas.JavaExP.utils.ConstantUtils;

public class ServiceModel extends ComponentModel  {
	private static final long serialVersionUID = 3L;

	public ServiceModel(AppModel appModel) {
		super(appModel);
		type = "s";
	}

	@Override
	public String getComponentType() {
		return ConstantUtils.SERVICE;
	}
}
