package com.techsure.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;

/*
 * @Description:
 * @Author: chenqiwei
 * @Date: 2021/2/9 10:31 下午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface MyApiComponent extends IApiComponent {
    Object myDoService(JSONObject jsonObj) throws Exception;
}
