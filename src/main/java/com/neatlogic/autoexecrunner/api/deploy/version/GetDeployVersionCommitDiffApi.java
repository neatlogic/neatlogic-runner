/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neatlogic.autoexecrunner.api.deploy.version;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author lvzk
 * @since 2023/8/4 14:31
 **/
@Component
public class GetDeployVersionCommitDiffApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取发布对应版本commit diff内容";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id", isRequired = true),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id", isRequired = true),
            @Param(name = "version", type = ApiParamType.STRING, desc = "版本", isRequired = true)

    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long appId = jsonObj.getLong("appSystemId");
        Long moduleId = jsonObj.getLong("appModuleId");
        String version = jsonObj.getString("version");
        String logPath = Config.DATA_HOME() + File.separator + appId+ File.separator + moduleId+ "artifact" + File.separator + version+ "diff.json";
        return FileUtil.getReadFileContent(logPath);
    }

    @Override
    public String getToken() {
        return "/deploy/version/commit/diff/get";
    }
}
