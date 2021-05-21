package com.techsure.autoexecproxy.validator;

import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.param.validate.core.ApiParamValidatorBase;

public class BooleanApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "布尔型";
	}

	@Override
	public boolean validate(Object param, String rule) {
		if(Boolean.TRUE.toString().equalsIgnoreCase(param.toString()) || Boolean.FALSE.toString().equalsIgnoreCase(param.toString())){
			return true;
		}else {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.BOOLEAN;
	}

}
