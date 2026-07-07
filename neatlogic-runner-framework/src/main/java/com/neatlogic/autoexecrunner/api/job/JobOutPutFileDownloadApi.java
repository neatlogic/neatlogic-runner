package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        String jobPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder());
        File outputFile = FileSafeUtil.getDownloadFile(paramObj.getString("path"), jobPath);
        response.setHeader("Content-Disposition", " attachment; filename=\"" + outputFile.getName() + "\"");
        FileUtil.downloadFileByPath(outputFile.getPath(), response);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/output/file/download";
    }
}
