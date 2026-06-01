package com.neatlogic.autoexecrunner.service;

import com.alibaba.fastjson.JSONObject;

public interface AutoexecJobService {

    /**
     * 获取作业工具入参
     *
     * @param jsonObj 入参
     * @return 作业工具入参
     */
    JSONObject getJobOperationInputParam(JSONObject jsonObj);
}
