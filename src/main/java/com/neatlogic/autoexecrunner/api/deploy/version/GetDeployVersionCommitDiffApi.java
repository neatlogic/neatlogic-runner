/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
        String path = Config.DATA_HOME() + File.separator + appId + File.separator + moduleId + File.separator + "artifact" + File.separator + version + File.separator + "diff.json";
        return FileUtil.getReadFileContent(path);
    }

    @Override
    public String getToken() {
        return "/deploy/version/commit/diff/get";
    }
}
