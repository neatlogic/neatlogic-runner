/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.service.AutoexecJobService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/11/23 16:31
 **/
@Component
public class JobPhaseNodeInputParamGetApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "获取作业节点输入参数";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "阶段名", isRequired = true),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return autoexecJobService.getJobOperationInputParam(jsonObj);
    }

    @Override
    public String getToken() {
        return "/job/phase/node/input/param/get";
    }
}
