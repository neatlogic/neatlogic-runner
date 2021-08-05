package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecproxy.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

@Component
public class JobPhaseNodeLogDownloadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/job/phase/node/log/download";
    }

    @Override
    public String getName() {
        return "下载剧本节点执行日志";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "作业nodeId"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id", isRequired = true),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        String sqlName = jsonObj.getString("sqlName");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + phase + File.separator;
        if (Arrays.asList("target","runner_target").contains(execMode)) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".txt";
        }else if (Objects.equals(execMode, "sqlfile")) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + File.separator + sqlName + ".sql.txt";
        } else {
            logPath += "local-0-0.txt";
        }
        InputStream in = FileUtil.getInputStream(logPath);
        if (in != null) {
            OutputStream os = response.getOutputStream();
            IOUtils.copyLarge(in, os);
            if (os != null) {
                os.flush();
                os.close();
            }
            in.close();
        }
        return null;
    }

}
