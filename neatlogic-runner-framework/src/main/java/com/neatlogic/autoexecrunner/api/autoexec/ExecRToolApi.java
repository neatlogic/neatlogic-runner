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
import com.neatlogic.autoexecrunner.exception.autoexec.ExecRToolExecuteFailedException;
import com.neatlogic.autoexecrunner.exception.autoexec.ExecRToolOutputEmptyException;
import com.neatlogic.autoexecrunner.exception.autoexec.ExecRToolOutputInvalidException;
import com.neatlogic.autoexecrunner.exception.core.TooManyRequestsException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.annotation.QpsLimitKey;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Runner-side synchronous entry for direct autoexec script execution.
 */
@QpsLimitKey(value = "qps.limit.execrtool.exec", defaultValue = 20)
@Component
@DependsOn("config")
public class ExecRToolApi extends PrivateApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(ExecRToolApi.class);
    private static final long PROCESS_WAIT_SLICE_MILLIS = 200L;
    private static final long PROCESS_TERMINATE_WAIT_SECONDS = 5L;
    private final Semaphore executionSemaphore = new Semaphore(Config.EXECRTOOL_EXECUTION_MAX_CONCURRENT());

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
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "跨步骤执行上下文id"),
            @Param(name = "reserveRunPath", type = ApiParamType.BOOLEAN, desc = "是否保留执行目录"),
            @Param(name = "cleanupContext", type = ApiParamType.BOOLEAN, desc = "是否仅清理跨步骤执行上下文"),
            @Param(name = "execrtoolSerialId", type = ApiParamType.LONG, desc = "execrtool流水id"),
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
        traceParam.put("jobId", jsonObj.getLong("jobId"));
        traceParam.put("cleanupContext", jsonObj.getBoolean("cleanupContext"));
        traceParam.put("execrtoolSerialId", jsonObj.getLong("execrtoolSerialId"));
        traceParam.put("ip", jsonObj.getString("ip"));
        traceParam.put("resourceId", jsonObj.getLong("resourceId"));
        Path payloadFilePath = null;
        Path outputFilePath = null;
        Process process = null;
        boolean acquired = false;
        boolean errorLogged = false;
        logger.info("execrtool start, param={}", traceParam);
        try {
            acquired = executionSemaphore.tryAcquire();
            if (!acquired) {
                throw new TooManyRequestsException("execrtool并发执行数已达到上限，请稍后重试。");
            }

            // payload 写入临时文件，避免命令行参数过长或特殊字符影响 execrtool 启动。
            Path payloadDir = Paths.get(Config.AUTOEXEC_HOME(), "tmp", "execrtool");
            Files.createDirectories(payloadDir);
            String fileNamePrefix = UUID.randomUUID().toString();
            payloadFilePath = payloadDir.resolve(fileNamePrefix + ".json");
            outputFilePath = payloadDir.resolve(fileNamePrefix + ".out");
            Files.write(payloadFilePath, jsonObj.toJSONString().getBytes(StandardCharsets.UTF_8));
            Files.createFile(outputFilePath);

            // execrtool 是同步阻塞入口，runner 必须等进程结束后再返回 HTTP 响应。
            ProcessBuilder builder = new ProcessBuilder("execrtool", "--payloadfile", payloadFilePath.toString());
            builder.redirectErrorStream(true);
            builder.redirectOutput(outputFilePath.toFile());
            process = builder.start();
            waitForProcess(process, outputFilePath);
            int exitValue = process.exitValue();
            String output = Files.readString(outputFilePath, StandardCharsets.UTF_8);

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
                throw new ExecRToolExecuteFailedException(message);
            }
            logger.info("execrtool success, param={}, durationMs={}", traceParam, durationMs);
            return result.get("Return");
        } catch (Exception ex) {
            if (process != null && process.isAlive()) {
                terminateProcessTree(process);
            }
            if (!errorLogged) {
                logger.error("execrtool exception, param={}, durationMs={}, exceptionClass={}, stack={}",
                        traceParam, System.currentTimeMillis() - startTime, ex.getClass().getName(), Arrays.toString(ex.getStackTrace()));
            }
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw ex;
        } finally {
            cleanupTempFiles(payloadFilePath, outputFilePath);
            if (acquired) {
                executionSemaphore.release();
            }
        }
    }

    /**
     * Wait for execrtool in short slices so runtime and redirected output remain bounded.
     */
    private void waitForProcess(Process process, Path outputFilePath) throws Exception {
        long maxRuntimeNanos = TimeUnit.SECONDS.toNanos(Config.EXECRTOOL_EXECUTION_MAX_RUNTIME_SECONDS());
        long maxOutputBytes = Math.multiplyExact(Config.EXECRTOOL_EXECUTION_MAX_OUTPUT_KB().longValue(), 1024L);
        long deadlineNanos = System.nanoTime() + maxRuntimeNanos;
        while (true) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                terminateProcessTree(process);
                throw new ExecRToolExecuteFailedException("execrtool执行超时，最大执行时长为"
                        + Config.EXECRTOOL_EXECUTION_MAX_RUNTIME_SECONDS() + "秒。");
            }
            long waitNanos = Math.min(TimeUnit.MILLISECONDS.toNanos(PROCESS_WAIT_SLICE_MILLIS), remainingNanos);
            boolean finished = process.waitFor(waitNanos, TimeUnit.NANOSECONDS);
            long outputBytes = Files.size(outputFilePath);
            if (outputBytes > maxOutputBytes) {
                terminateProcessTree(process);
                throw new ExecRToolExecuteFailedException("execrtool输出超过上限"
                        + Config.EXECRTOOL_EXECUTION_MAX_OUTPUT_KB() + "KB。");
            }
            if (finished) {
                return;
            }
        }
    }

    /**
     * Terminate descendants before the wrapper process to avoid orphaned execrtool children.
     */
    private void terminateProcessTree(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        try {
            process.descendants().forEach(processHandle -> {
                try {
                    processHandle.destroyForcibly();
                } catch (Exception ex) {
                    logger.error("Terminate execrtool child process failed, pid={}", processHandle.pid(), ex);
                }
            });
            process.destroyForcibly();
            if (!process.waitFor(PROCESS_TERMINATE_WAIT_SECONDS, TimeUnit.SECONDS)) {
                logger.error("Terminate execrtool process timed out, pid={}", process.pid());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("Wait for execrtool process termination interrupted, pid={}", process.pid(), ex);
        } catch (Exception ex) {
            logger.error("Terminate execrtool process failed, pid={}", process.pid(), ex);
        }
    }

    /**
     * Remove request-scoped files without masking the real execution result.
     */
    private void cleanupTempFiles(Path payloadFilePath, Path outputFilePath) {
        Path parentPath = payloadFilePath != null ? payloadFilePath.getParent()
                : outputFilePath != null ? outputFilePath.getParent() : null;
        try {
            if (payloadFilePath != null) {
                Files.deleteIfExists(payloadFilePath);
            }
            if (outputFilePath != null) {
                Files.deleteIfExists(outputFilePath);
            }
            if (parentPath != null) {
                File[] files = parentPath.toFile().listFiles();
                if (files != null && files.length == 0) {
                    Files.deleteIfExists(parentPath);
                }
            }
        } catch (Exception ex) {
            logger.error("Clean execrtool temporary files failed, payloadFile={}, outputFile={}",
                    payloadFilePath, outputFilePath, ex);
        }
    }

    /**
     * Parse execrtool stdout into JSON and convert invalid output into API errors.
     */
    private JSONObject parseResult(String output, int exitValue) {
        if (StringUtils.isBlank(output)) {
            throw new ExecRToolOutputEmptyException(exitValue);
        }
        try {
            return JSON.parseObject(output.trim());
        } catch (Exception ex) {
            throw new ExecRToolOutputInvalidException(exitValue, output, ex);
        }
    }

    /**
     * Keep log messages short and single-line so execrtool.log stays searchable.
     */
    private String sanitizeLogMessage(String message) {
        return StringUtils.abbreviate(StringUtils.defaultString(message).replaceAll("[\\r\\n]+", " "), 500);
    }
}
