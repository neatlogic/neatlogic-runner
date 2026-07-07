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
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import com.neatlogic.autoexecrunner.util.FolderZipperUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
        String version = jsonObj.getString("version");
        Integer buildNo = jsonObj.getInteger("buildNo");

        String artifactPath = Config.DATA_HOME() + File.separator + sysId + File.separator + moduleId + File.separator + "artifact";
        String appbuildRelativePath = version + File.separator + "build" + File.separator + buildNo;
        File appbuildFile;
        try {
            appbuildFile = FileSafeUtil.getValidatedDirectory(appbuildRelativePath, artifactPath);
        } catch (IllegalArgumentException | IOException ex) {
            throw new ApiRuntimeException("appbuild dir:" + appbuildRelativePath + " doesn't exists on server.");
        }
        String appbuildPath = appbuildFile.getPath();
        JSONArray subDirsParam = jsonObj.getJSONArray("subDirs");
        if (CollectionUtils.isEmpty(subDirsParam)) {
            subDirsParam = new JSONArray();
            subDirsParam.add(".");
        }
        List<String> subDirParamList = subDirsParam.toJavaList(String.class);
        List<String> subDirList = new ArrayList<>();
        for (String subDir : subDirParamList) {
            subDirList.add(FileSafeUtil.getValidatedRelativePath(appbuildPath, subDir));
        }
        ServletContext ctx = request.getServletContext();
        String mimeType = ctx.getMimeType(appbuildFile.getAbsolutePath());
        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + URLEncoder.encode(appbuildFile.getName() + ".tar.gz", "UTF-8") + "\"");
        ServletOutputStream os = response.getOutputStream();
        try {
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
