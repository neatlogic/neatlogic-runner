package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Component
public class JobOutPutFileDownloadApi extends PrivateBinaryStreamApiComponentBase {
    private static final Log logger = LogFactory.getLog(JobOutPutFileDownloadApi.class);

    @Override
    public String getName() {
        return "下载作业输出文件";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "path", type = ApiParamType.STRING, desc = "参数文件相对路径", isRequired = true)
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String outputFilePath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + paramObj.getString("path");
        File outputFile = new File(outputFilePath);
        if(outputFile.exists() && outputFile.isFile()) {
            response.setHeader("Content-Disposition", " attachment; filename=\"" + outputFile.getName() + "\"");
            FileUtil.downloadFileByPath(outputFilePath, response);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/output/file/download";
    }
}
