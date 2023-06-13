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
package com.neatlogic.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.file.FileDownloadException;
import com.neatlogic.autoexecrunner.exception.file.FileIsNotDirectoryException;
import com.neatlogic.autoexecrunner.exception.file.FileNotAllowedDownloadException;
import com.neatlogic.autoexecrunner.exception.file.FileNotFoundException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileDownloadApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileDownloadApi.class);

    @Override
    public String getToken() {
        return "/file/download";
    }

    @Override
    public String getName() {
        return "下载文件";
    }

    @Input({
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径", isRequired = true),
            @Param(name = "isPack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否打包"),
            @Param(name = "fileName", type = ApiParamType.STRING, desc = "文件名(未指定则默认使用文件自身的名称)")
    })
    @Output({})
    @Description(desc = "下载文件(若选择打包下载，下载的文件为压缩包)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = paramObj.getString("path");
        Integer isPack = paramObj.getInteger("isPack");
        String fileName = paramObj.getString("fileName");
        path = FileUtil.getFullAbsolutePath(path);
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        if (Objects.equals(isPack, 1) && !file.isDirectory()) {
            throw new FileIsNotDirectoryException(path);
        }
        fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"),
                (StringUtils.isBlank(fileName) ? Paths.get(path).getFileName().toString() : fileName) + (Objects.equals(isPack, 1) ? ".zip" : ""));
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        response.setContentType("application/octet-stream");
        if (Objects.equals(isPack, 1)) {
            try (ServletOutputStream os = response.getOutputStream()) {
                FileUtil.zipDirectory(path, os);
            } catch (Exception ex) {
                logger.error("download and pack file:" + path + " failed.", ex);
                throw new FileDownloadException(path);
            }
        } else {
            // 不允许下载目录和git配置文件
            if (file.isDirectory() || file.getAbsolutePath().endsWith(".git/config")) {
                throw new FileNotAllowedDownloadException(path);
            }
            try (ServletOutputStream os = response.getOutputStream(); FileInputStream is = new FileInputStream(file)) {
                IOUtils.copy(is, os);
            } catch (Exception ex) {
                logger.error("download file:" + path + " failed.", ex);
                throw new FileDownloadException(path);
            }
        }
        return null;
    }
}
