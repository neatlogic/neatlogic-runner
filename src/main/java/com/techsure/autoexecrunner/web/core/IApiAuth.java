package com.techsure.autoexecrunner.web.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.ApiVo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface IApiAuth {

    AuthenticateType getType();

    int auth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException;

    JSONObject help();

}
