package com.techsure.autoexecproxy.validator;


import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.param.validate.core.ApiParamValidatorBase;

import java.util.regex.Pattern;

public class IPApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {

		return "ip";
	}

	@Override
	public boolean validate(Object param, String rule) {
		Pattern pattern = Pattern.compile("^((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]" + "|[*])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])$");
		return pattern.matcher(param.toString()).matches();
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.IP;
	}

}
