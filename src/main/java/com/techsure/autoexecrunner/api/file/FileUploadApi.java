/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.file.FileIsNotFileException;
import com.techsure.autoexecrunner.exception.file.FileUnpackException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
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
