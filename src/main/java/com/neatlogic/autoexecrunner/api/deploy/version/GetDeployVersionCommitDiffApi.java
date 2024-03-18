/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
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
