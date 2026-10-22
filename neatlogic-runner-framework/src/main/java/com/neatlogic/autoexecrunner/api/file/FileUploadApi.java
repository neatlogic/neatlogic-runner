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
package com.neatlogic.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.SafeArchiveUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            if (Objects.equals(unpack, 1)) {
                // 先校验原虚拟上传路径，再把压缩包放到临时目录，失败时不能覆盖或删除历史同名文件。
                File destinationFile = FileSafeUtil.getChildPath(Config.DATA_HOME(), FileUtil.getFullAbsolutePath(path));
                File parent = destinationFile.getParentFile();
                FileSafeUtil.validateFilePathAllowed(new File(Config.DATA_HOME()), parent);
                if (!parent.isDirectory()) {
                    throw new java.io.IOException("上传目标的父目录不存在");
                }
                if (destinationFile.isDirectory()) {
                    throw new java.io.IOException("上传目标不能是目录");
                }
                Path temporaryDirectory = Files.createTempDirectory("runner-upload-");
                Path archive = temporaryDirectory.resolve(destinationFile.getName());
                try {
                    try (InputStream input = multipartFile.getInputStream()) {
                        Files.copy(input, archive);
                    }
                    // 所有格式先在临时目录验证，禁止直接调用系统 tar 写入业务目录。
                    try {
                        SafeArchiveUtil.unpack(archive.toFile(), parent, "GBK");
                    } catch (java.nio.charset.MalformedInputException | java.nio.charset.UnmappableCharacterException ex) {
                        SafeArchiveUtil.unpack(archive.toFile(), parent, StandardCharsets.UTF_8.name());
                    }
                } finally {
                    Files.deleteIfExists(archive);
                    Files.deleteIfExists(temporaryDirectory);
                }
            } else {
                FileUtil.uploadFile(path, multipartFile.getInputStream());
            }
        }
        return null;
    }
}
