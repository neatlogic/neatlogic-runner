/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.phase;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.JobUtil;
import com.techsure.autoexecrunner.util.SocketUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class WriteJobPhaseSocketApi extends PrivateApiComponentBase {
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public String getName() {
        return "socket通知";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业Id", isRequired = true),
            @Param(name = "socketFileName", type = ApiParamType.STRING, desc = "socket文件", isRequired = true),
            @Param(name = "informParam", type = ApiParamType.JSONOBJECT, desc = "通知参数", isRequired = true)
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        JSONObject informParam = jsonObj.getJSONObject("informParam");
        String socketFileName = jsonObj.getString("socketFileName");
        String pathToSocket = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + socketFileName + ".sock";
        SocketUtil.WriteAFUNIXDatagramSocket(pathToSocket, informParam);

        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/socket/write";
    }
}
