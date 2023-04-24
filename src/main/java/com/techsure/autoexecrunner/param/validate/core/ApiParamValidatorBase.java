package com.techsure.autoexecrunner.param.validate.core;


import com.techsure.autoexecrunner.constvalue.ApiParamType;

public abstract class ApiParamValidatorBase {
	public abstract String getName();

	public abstract boolean validate(Object param, String rule);

	public abstract ApiParamType getType();
}
