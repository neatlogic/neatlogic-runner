package com.techsure.autoexecrunner.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiAnonymousAccessSupportEnum;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

@Service
public class ServerHealthCheckApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "服务是否正常";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }

    @Override
    public String getToken() {
        return "server/health/check";
    }

    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }
}
