package com.techsure.autoexecproxy.web.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.AuthenticateType;
import com.techsure.autoexecproxy.dto.ApiVo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface IApiAuth {

    AuthenticateType getType();

    int auth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException;

    JSONObject help();

}
