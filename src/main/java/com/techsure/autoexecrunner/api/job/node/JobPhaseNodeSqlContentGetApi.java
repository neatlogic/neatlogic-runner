/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.JobUtil;
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
