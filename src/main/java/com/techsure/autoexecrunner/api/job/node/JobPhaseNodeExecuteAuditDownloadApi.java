/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.util.JobUtil;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Component
public class JobPhaseNodeExecuteAuditDownloadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/job/phase/node/execute/audit/download";
    }

    @Override
    public String getName() {
        return "下载作业节点执行记录";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "作业nodeId", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
            @Param(name = "startTime", type = ApiParamType.LONG, desc = "执行开始时间", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "执行状态", isRequired = true),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "执行用户", isRequired = true),
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long startTime = jsonObj.getLong("startTime");
        String execUser = jsonObj.getString("execUser");
        String phase = jsonObj.getString("phase");
        String ip = jsonObj.getString("ip");
        String sqlName = jsonObj.getString("sqlName");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String status = jsonObj.getString("status");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + phase + File.separator;
        if (Objects.equals(execMode, "sqlfile") && StringUtils.isNotBlank(sqlName)) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + File.separator + sqlName + ".hislog" + File.separator;
        } else {
            if (Arrays.asList("target", "runner_target").contains(execMode)) {
                logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".hislog" + File.separator;
            } else {
                logPath += "local-0-0.hislog" + File.separator;
            }
        }
        logPath += TimeUtil.convertDateToString(new Date(startTime), TimeUtil.YYYYMMDD_HHMMSS) + "." + status + "." + execUser + ".txt";
        FileUtil.downloadFileByPath(logPath, response);
        return null;
    }

}
