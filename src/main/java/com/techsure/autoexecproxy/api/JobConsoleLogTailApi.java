package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/5/31 11:31
 **/
@Component
public class JobConsoleLogTailApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "实时获取作业console日志";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "logPos", type = ApiParamType.LONG, desc = "读取下标", isRequired = true),
            @Param(name = "direction", type = ApiParamType.STRING, desc = "读取方向", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long logPos = jsonObj.getLong("logPos");
        String direction = jsonObj.getString("direction");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + "console.txt";
        return FileUtil.tailLog(logPath, logPos, direction);
    }

    @Override
    public String getToken() {
        return "/job/console/log/tail";
    }
}
