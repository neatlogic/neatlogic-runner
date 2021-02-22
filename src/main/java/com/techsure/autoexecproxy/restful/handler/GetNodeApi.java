package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

/**
 * @Title: AutoExecApi
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: 执行接口
 * @author: chenqiwei
 * @date: 2021/2/2210:44 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class GetNodeApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取执行节点接口";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String param = "[{ \"nodeId\": 456, \"nodeType\": \"ssh\", \"host\": \"192.168.0.168\", \"port\": 22, \"username\": \"root\", \"password\": \"techsure\" },\n" +
                "{ \"nodeId\": 567, \"nodeType\": \"tagent\", \"host\": \"192.168.0.22\", \"port\": 3939, \"username\": \"root\", \"password\": \"H0TURDsZLMreCo3U\" },\n" +
                "{ \"nodeId\": 458, \"nodeType\": \"tagent\", \"host\": \"192.168.0.26\", \"port\": 3939, \"username\": \"root\", \"password\": \"ts9012501\" }]";
        return JSONArray.parse(param);
    }

    @Override
    public boolean isRaw() {
        return true;
    }

    @Override
    public String getToken() {
        return "getnode";
    }
}
