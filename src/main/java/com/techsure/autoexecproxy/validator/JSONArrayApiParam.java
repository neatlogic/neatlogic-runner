package com.techsure.autoexecproxy.validator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.param.validate.core.ApiParamValidatorBase;

public class JSONArrayApiParam extends ApiParamValidatorBase {

	@Override
	public String getName() {

		return "json数组";
	}

	@Override
	public boolean validate(Object param, String rule) {
		try {
			JSONArray.parseArray(JSONObject.toJSONString(param));
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public ApiParamType getType() {
		return ApiParamType.JSONARRAY;
	}

}
