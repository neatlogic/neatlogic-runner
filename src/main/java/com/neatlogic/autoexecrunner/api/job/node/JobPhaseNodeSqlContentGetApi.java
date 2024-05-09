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

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author lvzk
 * @since 2021/5/13 14:31
 **/
@Component
public class JobPhaseNodeSqlContentGetApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取作业节点sql文件内容";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业阶段", isRequired = true),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名", isRequired = true),
            @Param(name = "encoding", type = ApiParamType.STRING, desc = "字符编码", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        //非发布sql文件路径  /app/autoexec/data/job/638/151/568/343/048/sqlfile/execSql/test.sql
        return FileUtil.getSqlFileContent(Config.AUTOEXEC_HOME() + File.separator
                + JobUtil.getJobPath(jsonObj.getLong("jobId").toString(), new StringBuilder())
                + File.separator + "sqlfile" + File.separator + jsonObj.getString("phase")
                + File.separator + jsonObj.getString("sqlName"), jsonObj.getString("encoding"));
    }

    @Override
    public String getToken() {
        return "/job/phase/node/sql/content/get";
    }
}
