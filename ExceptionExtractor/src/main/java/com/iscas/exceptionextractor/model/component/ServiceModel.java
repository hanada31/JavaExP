package com.iscas.exceptionextractor.model.component;

import com.iscas.exceptionextractor.model.analyzeModel.AppModel;
import com.iscas.exceptionextractor.utils.ConstantUtils;

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
