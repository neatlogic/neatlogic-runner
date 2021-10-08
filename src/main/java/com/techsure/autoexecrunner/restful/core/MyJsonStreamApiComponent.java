package com.techsure.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;

/*
 * @Description:
 * @Author: chenqiwei
 * @Date: 2021/2/9 10:32 下午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface MyJsonStreamApiComponent extends IJsonStreamApiComponent {
    Object myDoService(JSONObject paramObj, JSONReader jsonReader) throws Exception;
}
