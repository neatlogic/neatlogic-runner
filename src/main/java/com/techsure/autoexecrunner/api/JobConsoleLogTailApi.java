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
import org.springframework.stereotype.Component;

import java.io.File;

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
        String logPath = Config.AUTOEXEC_HOME() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + "console.txt";
        return FileUtil.tailLog(logPath, logPos, direction);
    }

    @Override
    public String getToken() {
        return "/job/console/log/tail";
    }
}
