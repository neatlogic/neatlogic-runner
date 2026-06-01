package com.neatlogic.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.utils.SonarQubeScanTaskManager;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

@Service
public class SonarQubeScanStatusApi extends PrivateApiComponentBase {
    @Override
    public String getToken() {
        return "codehub/sonarqube/scan/status";
    }

    @Override
    public String getName() {
        return "查询SonarQube代码扫描状态";
    }

    @Input({
            @Param(name = "taskId", type = ApiParamType.STRING, desc = "扫描任务id")
    })
    @Description(desc = "查询SonarQube代码扫描状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return SonarQubeScanTaskManager.getStatus(jsonObj.getString("taskId"));
    }
}
