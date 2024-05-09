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
