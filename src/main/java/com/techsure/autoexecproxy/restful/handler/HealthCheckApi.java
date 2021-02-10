package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.restful.annotation.Description;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Title: CheckHandler
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: 健康检查
 * @author: chenqiwei
 * @date: 2021/2/1010:54 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class HealthCheckApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckApi.class);


    @Override
    public String getName() {
        return "健康检查";
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增模型")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增模型")})
    @Description(desc = "健康检查接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        logger.error("FAILED");
        return null;
    }

    @Override
    public String getToken() {
        return "check";
    }
}
