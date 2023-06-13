/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neatlogic.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Component
public class JobPhaseNodeOutPutDownloadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/job/phase/node/output/download";
    }

    @Override
    public String getName() {
        return "下载剧本节点执行日志";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String sqlName = jsonObj.getString("sqlName");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "output" + File.separator;
        if (Arrays.asList("target", "runner_target").contains(execMode)) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".json";
        } else if (Objects.equals(execMode, "sqlfile")) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + File.separator + sqlName + ".json";
        } else {
            logPath += "local-0-0.json";
        }
        FileUtil.downloadFileByPath(logPath, response);
        return null;
    }

}
