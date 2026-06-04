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

package com.neatlogic.autoexecrunner.api.autoexec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * Runner-side synchronous entry for direct autoexec script execution.
 */
@Component
public class ExecRToolApi extends PrivateApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(ExecRToolApi.class);

    /**
     * Return runner private API token used by NeatLogic to call execrtool.
     */
    @Override
    public String getToken() {
        return "/execrtool/exec";
    }

    /**
     * Return display name for the runner API.
     */
    @Override
    public String getName() {
        return "直接通过tagent执行自定义脚本";
    }

    /**
     * Execute bin/execrtool synchronously and return its final JSON payload.
     */
    @Input({
            @Param(name = "tenant", type = ApiParamType.STRING, desc = "租户", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "执行器映射id", isRequired = true),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "执行用户", isRequired = true),
            @Param(name = "execId", type = ApiParamType.STRING, desc = "执行id", isRequired = true),
            @Param(name = "execrtoolSerialId", type = ApiParamType.LONG, desc = "execrtool流水id", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "目标ip"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id")
    })
    @Output({
            @Param(type = ApiParamType.JSONOBJECT, desc = "脚本出参")
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        long startTime = System.currentTimeMillis();
        JSONObject traceParam = new JSONObject();
        traceParam.put("tenant", jsonObj.getString("tenant"));
        traceParam.put("runnerId", jsonObj.getLong("runnerId"));
        traceParam.put("execUser", jsonObj.getString("execUser"));
        traceParam.put("execId", jsonObj.getString("execId"));
        traceParam.put("execrtoolSerialId", jsonObj.getLong("execrtoolSerialId"));
        traceParam.put("ip", jsonObj.getString("ip"));
        traceParam.put("resourceId", jsonObj.getLong("resourceId"));
        Path payloadFilePath = null;
        boolean errorLogged = false;
        logger.info("execrtool start, param={}", traceParam);
        try {
            // payload 写入临时文件，避免命令行参数过长或特殊字符影响 execrtool 启动。
            Path payloadDir = Paths.get(Config.AUTOEXEC_HOME(), "tmp", "execrtool");
            Files.createDirectories(payloadDir);
            payloadFilePath = payloadDir.resolve(UUID.randomUUID() + ".json");
            Files.write(payloadFilePath, jsonObj.toJSONString().getBytes(StandardCharsets.UTF_8));

            // execrtool 是同步阻塞入口，runner 必须等进程结束后再返回 HTTP 响应。
            ProcessBuilder builder = new ProcessBuilder("execrtool", "--payloadfile", payloadFilePath.toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            int exitValue = process.waitFor();

            JSONObject result = parseResult(output, exitValue);
            long durationMs = System.currentTimeMillis() - startTime;
            String resultStatus = result.getString("Status");
            logger.info("execrtool process finish, param={}, exitValue={}, durationMs={}, outputLength={}, resultStatus={}",
                    traceParam, exitValue, durationMs, output == null ? 0 : output.length(), resultStatus);
            if (!"OK".equals(resultStatus)) {
                String message = result.getString("Message");
                Object resultReturn = result.get("Return");
                logger.error("execrtool failed, param={}, exitValue={}, durationMs={}, resultStatus={}, errorMessage={}",
                        traceParam, exitValue, durationMs, resultStatus, sanitizeLogMessage(message));
                errorLogged = true;
                if (resultReturn != null) {
                    // 失败明细透传给 NeatLogic，便于直接在接口响应中排查。
                    message = StringUtils.defaultString(message) + ", detail: " + JSON.toJSONString(resultReturn);
                }
                throw new ApiRuntimeException(message);
            }
            logger.info("execrtool success, param={}, durationMs={}", traceParam, durationMs);
            return result.get("Return");
        } catch (Exception ex) {
            if (!errorLogged) {
                logger.error("execrtool exception, param={}, durationMs={}, exceptionClass={}, stack={}",
                        traceParam, System.currentTimeMillis() - startTime, ex.getClass().getName(), Arrays.toString(ex.getStackTrace()));
            }
            throw ex;
        } finally {
            if (payloadFilePath != null) {
                Files.deleteIfExists(payloadFilePath);
                File parentFile = payloadFilePath.getParent().toFile();
                File[] files = parentFile.listFiles();
                if (files != null && files.length == 0) {
                    Files.deleteIfExists(payloadFilePath.getParent());
                }
            }
        }
    }

    /**
     * Parse execrtool stdout into JSON and convert invalid output into API errors.
     */
    private JSONObject parseResult(String output, int exitValue) {
        if (StringUtils.isBlank(output)) {
            throw new ApiRuntimeException("execrtool returned empty output, exitValue: " + exitValue);
        }
        try {
            return JSON.parseObject(output.trim());
        } catch (Exception ex) {
            throw new ApiRuntimeException("execrtool returned invalid json, exitValue: " + exitValue + ", output: " + output);
        }
    }

    /**
     * Keep log messages short and single-line so execrtool.log stays searchable.
     */
    private String sanitizeLogMessage(String message) {
        return StringUtils.abbreviate(StringUtils.defaultString(message).replaceAll("[\\r\\n]+", " "), 500);
    }
}
