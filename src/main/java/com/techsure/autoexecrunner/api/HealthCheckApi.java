package com.techsure.autoexecrunner.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

/**
 * @author chenqiwei
 * @since 2021/2/1010:54 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class HealthCheckApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "健康检查";
    }

    @Description(desc = "健康检查接口")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return null;
    }

    @Override
    public String getToken() {
        return "/health/check";
    }
}
