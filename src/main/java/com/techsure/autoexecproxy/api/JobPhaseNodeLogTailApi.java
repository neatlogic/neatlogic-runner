package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.dto.FileTailerVo;
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
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "position", type = ApiParamType.LONG, desc = "读取下标", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口", isRequired = true),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
            @Param(name = "direction", type = ApiParamType.STRING, desc = "读取方向", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        Long position = jsonObj.getLong("position");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port");
        String direction = jsonObj.getString("direction");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator+phase + File.separator ;
        if(Objects.equals(execMode,"target")){
            logPath +=  ip + "-" + port + ".text";
        }else{
            logPath += "local-0.text";
        }

        return FileUtil.tailLog(logPath, position, direction);
    }

    @Override
    public String getToken() {
        return "/job/phase/node/log/tail";
    }
}
