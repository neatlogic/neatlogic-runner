package com.techsure.autoexecproxy.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.dto.ApiVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IBinaryStreamApiComponent {

    String getId();

    String getName();

    // true时返回格式不再包裹固定格式
    default boolean isRaw() {
        return false;
    }


    Object doService(ApiVo interfaceVo, JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception;

    JSONObject help();
}
