package com.neatlogic.autoexecrunner.util.authtication.handler;


import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.util.authtication.core.IAuthenticateHandler;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;
import java.util.Base64;

public class BasicAuthenticateHandler implements IAuthenticateHandler {

	@Override
	public String getType() {
		return AuthenticateType.BASIC.getValue();
	}

	@Override
	public void authenticate(HttpURLConnection connection, RestVo rest) {
		String username = rest.getUsername();
		String password = rest.getPassword();
		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
			Base64.Encoder encoder = Base64.getEncoder();
			String key = username + ":" + password;
			connection.addRequestProperty("Authorization", "Basic " + encoder.encodeToString(key.getBytes()));
		}
	}

}
