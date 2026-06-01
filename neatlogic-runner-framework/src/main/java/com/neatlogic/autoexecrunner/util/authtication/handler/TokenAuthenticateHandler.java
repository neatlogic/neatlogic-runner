package com.neatlogic.autoexecrunner.util.authtication.handler;


import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.util.authtication.core.IAuthenticateHandler;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;

public class TokenAuthenticateHandler implements IAuthenticateHandler {
	@Override
	public String getType() {
		return AuthenticateType.BEARER.getValue();
	}

	@Override
	public void authenticate(HttpURLConnection connection, RestVo rest) {
		String token = rest.getToken();
		if (StringUtils.isNotBlank(token)) {
			connection.addRequestProperty("Authorization", token);
		}
	}
}
