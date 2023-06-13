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
import com.neatlogic.autoexecrunner.exception.file.FileIsNotFileException;
import com.neatlogic.autoexecrunner.exception.file.FileUnpackException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileUploadApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileUploadApi.class);

    @Override
    public String getToken() {
        return "/file/upload";
    }

    @Override
    public String getName() {
        return "上传文件";
    }

    @Input({
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径(需要指定文件名)", isRequired = true),
            @Param(name = "unpack", type = ApiParamType.ENUM, rule = "1,0", desc = "是否解压(1:是;0:否)"),
    })
    @Output({
            @Param(name = "success", type = ApiParamType.INTEGER, desc = "是否成功(1:是;0:否)")
    })
    @Description(desc = "上传文件(若选择上传后解压文件，解压成功会删除原文件)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = paramObj.getString("path");
        Integer unpack = paramObj.getInteger("unpack");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        if (MapUtils.isNotEmpty(fileMap)) {
            MultipartFile multipartFile = fileMap.entrySet().stream().findFirst().get().getValue();
            String filePath = FileUtil.uploadFile(path, multipartFile.getInputStream());
            if (Objects.equals(unpack, 1)) {
                File file = new File(filePath);
                if (!file.isFile()) {
                    throw new FileIsNotFileException(filePath);
                }
                String parentDir = file.getParent();
                String fileName = file.getName();
                String packName = fileName.toLowerCase();
                String cmd = null;
                String opts = null;
                if (packName.endsWith(".tgz") || packName.endsWith(".tar.gz")) {
                    cmd = "tar";
                    opts = "-xzf";
                } else if (packName.endsWith(".tar")) {
                    cmd = "tar";
                    opts = "-xf";
                }
                // zip 文件使用 java 解压
                if ("tar".equals(cmd)) {
                    ProcessBuilder builder = new ProcessBuilder(cmd, opts, fileName);
                    builder.directory(new File(parentDir));
                    Process proc = builder.start();
                    proc.waitFor();
                    int exitValue = proc.exitValue();
                    file.delete();
                    if (exitValue != 0) {
                        throw new FileUnpackException(filePath, cmd + " " + opts);
                    }
                } else {
                    try {
                        // 优先使用 GBK 解码 zip 文件，解码失败则使用 UTF-8解码
                        FileUtil.unzipFile(file, "GBK");
                    } catch (Exception ex) {
                        FileUtil.unzipFile(file, StandardCharsets.UTF_8.name());
                    } finally {
                        file.delete();
                    }
                }
            }
        }
        return null;
    }
}
