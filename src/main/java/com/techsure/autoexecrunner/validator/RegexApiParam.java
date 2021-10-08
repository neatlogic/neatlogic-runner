package com.techsure.autoexecrunner.validator;


import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.param.validate.core.ApiParamValidatorBase;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class RegexApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "正则表达式";
	}

	@Override
	public boolean validate(Object param, String rule) {
		if (StringUtils.isNotBlank(rule)) {
			Pattern pattern = Pattern.compile(rule);
			return pattern.matcher(param.toString()).matches();
		} else {
			return true;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.REGEX;
	}

}
