package com.techsure.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * @Description:
 * @Author: chenqiwei
 * @Date: 2021/2/9 10:32 下午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface MyBinaryStreamApiComponent extends IBinaryStreamApiComponent {
    Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
