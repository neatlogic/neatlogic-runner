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
public class SonarQubeScanStartApi extends PrivateApiComponentBase {
    @Override
    public String getToken() {
        return "codehub/sonarqube/scan/start";
    }

    @Override
    public String getName() {
        return "发起SonarQube代码扫描";
    }

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "repositoryServiceId", type = ApiParamType.STRING, desc = "仓库服务id"),
            @Param(name = "repoPath", type = ApiParamType.STRING, desc = "仓库路径"),
            @Param(name = "sonarConfig", type = ApiParamType.JSONOBJECT, desc = "SonarQube扫描配置")
    })
    @Description(desc = "发起SonarQube代码扫描")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return SonarQubeScanTaskManager.start(jsonObj);
    }
}
