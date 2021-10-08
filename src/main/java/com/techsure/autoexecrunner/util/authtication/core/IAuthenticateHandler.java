package com.techsure.autoexecrunner.util.authtication.core;

import com.alibaba.fastjson.JSONObject;

import java.net.HttpURLConnection;

public interface IAuthenticateHandler {
	public String getType();

	public void authenticate(HttpURLConnection connection, JSONObject config);
}
