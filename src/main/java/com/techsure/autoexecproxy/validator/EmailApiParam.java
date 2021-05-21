package com.techsure.autoexecproxy.validator;


import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.param.validate.core.ApiParamValidatorBase;

import java.util.regex.Pattern;

public class EmailApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "邮箱地址";
	}

	@Override
	public boolean validate(Object param, String rule) {
		Pattern pattern = Pattern.compile("^[A-Za-z0-9]+([_\\.][A-Za-z0-9]+)*@([A-Za-z0-9\\-]+\\.)+[A-Za-z]{2,6}$");
		return pattern.matcher(param.toString()).matches();
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.EMAIL;
	}

}
