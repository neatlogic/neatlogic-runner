package com.neatlogic.autoexecrunner.validator;


import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.param.validate.core.ApiParamValidatorBase;

public class JSONObjectApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {
		return "json对象";
	}

	@Override
	public boolean validate(Object param, String rule) {
		try {
			JSONObject.parseObject(JSONObject.toJSONString(param));
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.JSONOBJECT;
	}

}
