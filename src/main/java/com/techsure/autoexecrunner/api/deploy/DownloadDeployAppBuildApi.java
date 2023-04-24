/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.deploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.util.FolderZipperUtil;
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
