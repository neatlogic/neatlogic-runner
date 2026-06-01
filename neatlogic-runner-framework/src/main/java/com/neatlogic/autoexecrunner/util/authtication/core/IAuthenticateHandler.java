package com.neatlogic.autoexecrunner.util.authtication.core;

import com.neatlogic.autoexecrunner.dto.RestVo;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;

public interface IAuthenticateHandler {
    String getType();

    void authenticate(HttpURLConnection connection, RestVo rest) throws MalformedURLException;
}
