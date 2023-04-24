/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.JobUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Component
public class GetJobPhaseOperationCustomDataApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "实时获取阶段工具个性化数据";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "jobPhaseOperationId", type = ApiParamType.LONG, desc = "阶段工具id", isRequired = true),
            @Param(name = "operationName", type = ApiParamType.STRING, desc = "工具名称", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long resourceId = jsonObj.getLong("resourceId");
        Long jobPhaseOperationId = jsonObj.getLong("jobPhaseOperationId");
        String operationName = jsonObj.getString("operationName");
        String phase = jsonObj.getString("phase");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "livedata" + File.separator + phase + File.separator;
        if (Arrays.asList("target", "runner_target").contains(execMode)) {
            logPath += ip + "-" + port + "-" + resourceId + File.separator + operationName + "_" + jobPhaseOperationId + ".json";
        } else if (Objects.equals(execMode, "runner")) {
            logPath += "local-0-0" + File.separator + operationName + "_" + jobPhaseOperationId + ".json";
        }
        return FileUtil.getReadFileContent(logPath);
    }

    @Override
    public String getToken() {
        return "/job/phase/operation/customdata/get";
    }
}
