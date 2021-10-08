package com.techsure.autoexecrunner.web.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.dto.ApiVo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public abstract class ApiAuthBase implements IApiAuth {


    @Override
    public int auth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException {
        return myAuth(interfaceVo,jsonParam,request);
    }

    public abstract int myAuth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException;

}
