package com.techsure.autoexecrunner.util.authtication.handler;


import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.util.authtication.core.IAuthenticateHandler;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;

public class BearerAuthenticateHandler implements IAuthenticateHandler {
	@Override
	public String getType() {
		return AuthenticateType.BEARER.getValue();
	}

	@Override
	public void authenticate(HttpURLConnection connection, JSONObject config) {
		String token = config.getString("token");
		if (StringUtils.isNotBlank(token)) {
			connection.addRequestProperty("Authorization", "Bearer " + token);
		}
	}
}
