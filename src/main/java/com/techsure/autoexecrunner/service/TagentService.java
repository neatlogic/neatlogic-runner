package com.techsure.autoexecrunner.service;

import com.alibaba.fastjson.JSONObject;

/**
 * @author lvzk
 * @since 2021/10/14 14:13
 **/
public interface TagentService {
    /**
     * @param jsonObj  参数
     * @param url      请求转发url
     * @param execInfo 执行信息
     * @return 执行转发结果
     */
    boolean forwardNeatlogicWeb(JSONObject jsonObj, String url, StringBuilder execInfo, boolean isFromTagent);
}
