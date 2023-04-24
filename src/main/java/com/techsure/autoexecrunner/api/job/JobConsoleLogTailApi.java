/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job;

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
            @Param(name = "direction", type = ApiParamType.STRING, desc = "读取方向", isRequired = true),
            @Param(name = "encoding", type = ApiParamType.STRING, desc = "字符编码", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "作业状态", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long logPos = jsonObj.getLong("logPos");
        String direction = jsonObj.getString("direction");
        String encoding = jsonObj.getString("encoding");
        String logPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "log" + File.separator + "console.txt";
        return FileUtil.tailLogWithoutHtml(logPath, logPos, direction, encoding, jsonObj.getString("status"));
    }

    @Override
    public String getToken() {
        return "/job/console/log/tail";
    }
}
