package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.JobUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author longrf
 * @date 2022/5/30 9:56 上午
 */
@Component
public class JobPhaseNodeSqlFileDownloadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "下载节点sql文件";
    }

    @Override
    public String getToken() {
        return "/job/phase/node/sql/file/download";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业阶段", isRequired = true),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名", isRequired = true)
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //非发布sql文件路径  /app/autoexec/data/job/638/151/568/343/048/sqlfile/execSql/test.sql
        FileUtil.downloadFileByPath(Config.AUTOEXEC_HOME() + File.separator
                        + JobUtil.getJobPath(paramObj.getLong("jobId").toString(), new StringBuilder())
                        + File.separator + "sqlfile" + File.separator + paramObj.getString("phase")
                        + File.separator + paramObj.getString("sqlName")
                , response);
        return null;
    }

}
