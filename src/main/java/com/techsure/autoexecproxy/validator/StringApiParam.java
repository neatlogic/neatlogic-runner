package com.techsure.autoexecproxy.validator;


import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.param.validate.core.ApiParamValidatorBase;

public class StringApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "字符串";
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.STRING;
	}

	@Override
	public boolean validate(Object param, String rule) {
		return true;
	}

}
