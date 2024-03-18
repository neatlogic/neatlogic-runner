/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@Component
public class TailJobPhaseBatchNodeLogApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/job/phase/batchnode/log/tail";
    }

    @Override
    public String getName() {
        return "实时批量获取剧本节点执行日志";
    }

    @Input({
            @Param(name = "nodeList", type = ApiParamType.JSONARRAY, desc = "节点列表", isRequired = true),
            @Param(name = "wordCountLimit", type = ApiParamType.INTEGER, desc = "限制字符数", isRequired = true),
            @Param(name = "nodeList.id", type = ApiParamType.LONG, desc = "节点id"),
            @Param(name = "nodeList.jobId", type = ApiParamType.LONG, desc = "作业Id"),
            @Param(name = "nodeList.resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "nodeList.sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "nodeList.phase", type = ApiParamType.STRING, desc = "作业剧本Name"),
            @Param(name = "nodeList.ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "nodeList.port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "nodeList.execMode", type = ApiParamType.STRING, desc = "执行方式"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray nodeList = jsonObj.getJSONArray("nodeList");
        int wordCountLimit = jsonObj.getIntValue("wordCountLimit");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        JSONWriter jsonWriter = new JSONWriter(outputStreamWriter);
        jsonWriter.startArray();
        for (int i = 0; i < nodeList.size(); i++) {
            JSONObject obj = nodeList.getJSONObject(i);
            String filePath = getFilePath(obj);
            String content = FileUtil.getFileContentWithLimit(filePath, wordCountLimit);
            JSONObject node = new JSONObject();
            node.put("id", obj.getLong("id"));
            node.put("content", content);
            jsonWriter.writeObject(node);
        }
        jsonWriter.endArray();
        jsonWriter.close();
        outputStreamWriter.close();
        return null;
    }

    private String getFilePath(JSONObject jsonObj) {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        String sqlName = jsonObj.getString("sqlName");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + phase + File.separator;
        if (Objects.equals(execMode, "sqlfile") && StringUtils.isNotBlank(sqlName)) {
            logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + File.separator + sqlName + ".txt";
        } else {
            if (Arrays.asList("target", "runner_target").contains(execMode)) {
                logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".txt";
            } else {
                logPath += "local-0-0.txt";
            }
        }
        return logPath;
    }

    public static void main(String[] args) throws IOException {
        String path = "C:\\Users\\Aieano\\Desktop\\user.xml";
        String content = FileUtil.getFileContentWithLimit(path, 10);
        System.out.println(content);
    }


}
