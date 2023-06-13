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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.dto.FileVo;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.JobUtil;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/5/13 14:31
 **/
@Component
public class JobPhaseNodeSqlListApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取作业节点sql列表";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "作业nodeId", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
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
        String phase = jsonObj.getString("phase");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator;
        logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId");
        List<FileVo> fileVoList = FileUtil.readFileList(logPath);
        JSONArray resultArray = new JSONArray();
        if (CollectionUtils.isNotEmpty(fileVoList)) {
            for (FileVo fileVo : fileVoList) {
                if (fileVo.getIsDirectory() == 0) {
                    String sqlStatusContent = FileUtil.getReadFileContent(fileVo.getFilePath());
                    JSONObject sqlStatus = JSONObject.parseObject(sqlStatusContent);
                    sqlStatus.put("sqlName", URLDecoder.decode(fileVo.getFileName().replaceAll(".txt", StringUtils.EMPTY), "UTF-8"));
                    Long startTime = sqlStatus.getLong("startTime");
                    if (startTime != null) {
                        sqlStatus.put("startTime", startTime * 1000);
                    }
                    Long endTime = sqlStatus.getLong("endTime");
                    if (endTime != null) {
                        sqlStatus.put("endTime", endTime * 1000);
                    }
                    resultArray.add(sqlStatus);
                }
            }
        }
        return resultArray;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/sql/list";
    }
}
