package com.techsure.autoexecrunner.validator;


import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.param.validate.core.ApiParamValidatorBase;

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
