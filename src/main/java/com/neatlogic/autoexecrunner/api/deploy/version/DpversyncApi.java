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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class DpversyncApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(DpversyncApi.class);

    private static final File NULL_FILE = new File("/dev/null");

    @Override
    public String getToken() {
        return "/deploy/dpversync";
    }

    @Override
    public String getName() {
        return "发布版本工程目录同步";
    }

    @Input({
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "该runner组下所有runner", isRequired = true),
            @Param(name = "targetPaths", type = ApiParamType.JSONARRAY, desc = "需要同步的目标路径", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "执行器id", isRequired = true)
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        try {
            JSONArray targetPaths = jsonObj.getJSONArray("targetPaths");
            ProcessBuilder builder = new ProcessBuilder("dpversync");
            builder.redirectOutput(NULL_FILE);
            builder.redirectError(NULL_FILE);
            Map<String, String> env = builder.environment();
            env.put("RUNNER_GROUP", jsonObj.getString("runnerGroup"));
            env.put("DEPLOY_TARGET_PATH", getParentDirectory(targetPaths).toJSONString());
            env.put("RUNNER_ID", jsonObj.getString("runnerId"));
            env.put("tenant", TenantContext.get().getTenantUuid());
            Process proc = builder.start();
            proc.waitFor();
            String msgError = IOUtils.toString(proc.getErrorStream());
            logger.error(msgError);
            result.put("msgError", msgError);
            result.put("exitValue", proc.exitValue());
            result.remove("password");
        } catch (Exception ex) {
            result.put("exitValue", 1);
            result.put("msgError", ex.getMessage());
            logger.error(ex.getMessage(), ex);
        }
        return result;
    }


    /**
     * 获取父文件夹
     *
     * @param targetPaths 目标文件路径
     */
    private JSONArray getParentDirectory(JSONArray targetPaths) {
        JSONArray targetParentPaths = new JSONArray();
        for (int i = 0; i < targetPaths.size(); i++) {
            String path = FileUtil.getFullAbsolutePath(targetPaths.getString(i));
            File file = new File(path);
            if (file.exists()) {
                targetParentPaths.add(file.getParentFile().getAbsolutePath());
            } else {
                path = path.substring(0, path.lastIndexOf("/"));
                file = new File(path);
                if (file.exists()) {
                    targetParentPaths.add(file.getAbsolutePath());
                }
            }
        }
        return FileUtil.deleteSonPath(targetParentPaths);
    }

}
