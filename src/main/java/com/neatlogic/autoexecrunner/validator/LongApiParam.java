package com.neatlogic.autoexecrunner.validator;


import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.param.validate.core.ApiParamValidatorBase;

public class LongApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "长整数";
	}

	@Override
	public boolean validate(Object param, String rule) {
		try {
			Long.valueOf(param.toString());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.LONG;
	}

}
