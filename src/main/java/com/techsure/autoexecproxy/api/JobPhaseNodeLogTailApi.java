package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.dto.FileTailerVo;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.File;

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

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long phase = jsonObj.getLong("phase");
        JSONObject nodeJson = jsonObj.getJSONObject("node");
        Long position = jsonObj.getLong("position");
        String ip = nodeJson.getString("ip");
        String port = nodeJson.getString("port");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + phase + File.separator + ip + "-" + port + ".text";
        return FileUtil.tailLog(logPath, position, "down");
    }

    @Override
    public String getToken() {
        return "/job/phase/node/log/tail";
    }
}
