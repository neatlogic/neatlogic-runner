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
package com.neatlogic.autoexecrunner.api.deploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FolderZipperUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/7/25 15:31
 **/
@Component
public class DownloadDeployAppBuildApi extends PrivateBinaryStreamApiComponentBase {
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public String getName() {
        return "下载 appbuild";
    }

    @Input({
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "系统名称"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名称"),
            @Param(name = "sysId", type = ApiParamType.LONG, isRequired = true, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "buildNo", type = ApiParamType.INTEGER, isRequired = true, desc = "build no"),
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "版本名称"),
            @Param(name = "subDirs", type = ApiParamType.JSONARRAY, desc = "subDirs"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long sysId = jsonObj.getLong("sysId");
        Long moduleId = jsonObj.getLong("moduleId");
        String envName = jsonObj.getString("envName");
        String version = jsonObj.getString("version");
        Integer buildNo = jsonObj.getInteger("buildNo");
        /*String subDirs = jsonObj.getString("subDirs");

        if (subDirs == null || "".equals(subDirs)) {
            subDirs = ".";
        } else {
            subDirs = subDirs.replace('\\', '/');
            subDirs = subDirs.replace("..", "");
            subDirs = subDirs.replace("//", "/");
        }*/

        String subSysPath = sysId + File.separator + moduleId;
        String appbuildPath = Config.DATA_HOME() + File.separator + subSysPath + File.separator + "artifact" + File.separator + version + File.separator + "build" + File.separator + buildNo;
        File appbuildFile = new File(appbuildPath);
        if (!appbuildFile.exists()) {
            throw new ApiRuntimeException("appbuild dir:" + appbuildPath + " doesn't exists on server.");
        }
        if (!appbuildFile.isDirectory()) {
            throw new ApiRuntimeException("appbuild:" + appbuildPath + " is not a directory.");
        }
        ServletContext ctx = request.getServletContext();
        String mimeType = ctx.getMimeType(appbuildFile.getAbsolutePath());
        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + URLEncoder.encode(appbuildFile.getName() + ".tar.gz", "UTF-8") + "\"");
        ServletOutputStream os = response.getOutputStream();
        try {
            JSONArray subDirsParam = jsonObj.getJSONArray("subDirs");
            if (CollectionUtils.isEmpty(subDirsParam)) {
                subDirsParam = new JSONArray();
                subDirsParam.add(".");
            }
            List<String> subDirList = subDirsParam.toJavaList(String.class);
            subDirList = subDirList.stream().map(o -> o.replace('\\', '/').replace("..", "").replace("//", "/")).collect(Collectors.toList());
            FolderZipperUtil.tgzFolderToResponse(appbuildPath, appbuildPath, subDirList, os);
        } catch (IOException ex) {
            try {
                logger.error(ex.getMessage(), ex);
                ex.printStackTrace(response.getWriter());
            } catch (Exception ignored) {
            }
        } finally {
            os.flush();
            os.close();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/appbuild/download";
    }
}
