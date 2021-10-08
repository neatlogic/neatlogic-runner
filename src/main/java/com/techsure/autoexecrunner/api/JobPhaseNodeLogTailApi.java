package com.techsure.autoexecrunner.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.core.ExecManager;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/5/13 14:31
 **/
@Component
public class JobPhaseNodeLogTailApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "实时获取剧本节点执行日志";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "作业nodeId", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "logPos", type = ApiParamType.LONG, desc = "读取下标", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
            @Param(name = "direction", type = ApiParamType.STRING, desc = "读取方向", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        String sqlName = jsonObj.getString("sqlName");
        Long logPos = jsonObj.getLong("logPos");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String direction = jsonObj.getString("direction");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + phase + File.separator;
        if (Objects.equals(execMode, "sqlfile") && StringUtils.isNotBlank(sqlName)) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + File.separator + URLEncoder.encode(sqlName,"UTF-8") + ".txt";
        } else {
            if (Arrays.asList("target", "runner_target", "sqlfile").contains(execMode)) {
                logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".txt";
            } else {
                logPath += "local-0-0.txt";
            }
        }

        return FileUtil.tailLog(logPath, logPos, direction);
    }

    @Override
    public String getToken() {
        return "/job/phase/node/log/tail";
    }
}
