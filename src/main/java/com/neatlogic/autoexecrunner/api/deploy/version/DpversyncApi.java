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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
            env.put("RUNNER_GROUP", jsonObj.getJSONObject("runnerGroup").toJSONString());
            env.put("DEPLOY_TARGET_PATH", getParentDirectory(targetPaths).toJSONString());
            env.put("RUNNER_ID", jsonObj.getString("runnerId"));
            env.put("tenant", TenantContext.get().getTenantUuid());
            Process proc = builder.start();
            proc.waitFor();
            String msgError = IOUtils.toString(proc.getErrorStream());
            if (StringUtils.isNotBlank(msgError)) {
                logger.error(msgError);
                result.put("msgError", msgError);
            }
            result.put("exitValue", proc.exitValue());
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
