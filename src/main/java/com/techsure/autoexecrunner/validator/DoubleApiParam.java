package com.techsure.autoexecrunner.validator;


import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.param.validate.core.ApiParamValidatorBase;

public class DoubleApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "双精度浮点数";
	}

	@Override
	public boolean validate(Object param, String rule) {
		try {
			Double.valueOf(param.toString());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.DOUBLE;
	}

}
