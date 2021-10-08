package com.techsure.autoexecrunner.validator;


import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.param.validate.core.ApiParamValidatorBase;
import org.apache.commons.lang3.StringUtils;

public class EnumApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "枚举";
	}

	@Override
	public boolean validate(Object param, String rule) {
		if (StringUtils.isNotBlank(rule)) {
			if (rule.contains(",")) {
				for (String r : rule.split(",")) {
					if (param.toString().equals(r)) {
						return true;
					}
				}
			} else {
				if (param.equals(rule)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.ENUM;
	}

}
