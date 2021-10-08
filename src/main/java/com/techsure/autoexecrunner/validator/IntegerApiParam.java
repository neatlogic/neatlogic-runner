package com.techsure.autoexecrunner.validator;

import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.param.validate.core.ApiParamValidatorBase;

public class IntegerApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "整数";
	}

	@Override
	public boolean validate(Object param, String rule) {
		try {
			Integer.valueOf(param.toString());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.INTEGER;
	}

}
