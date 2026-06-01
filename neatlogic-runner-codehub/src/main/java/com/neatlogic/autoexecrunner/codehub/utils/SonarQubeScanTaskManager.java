package com.neatlogic.autoexecrunner.codehub.utils;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.git.GitWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SonarQubeScanTaskManager {
    private static final int DEFAULT_TIMEOUT_SECONDS = 600;
    private static final long TASK_KEEP_MILLIS = 24L * 60 * 60 * 1000;
    private static final ConcurrentHashMap<String, JSONObject> TASK_MAP = new ConcurrentHashMap<>();
    private static final ExecutorService SCAN_EXECUTOR = Executors.newCachedThreadPool();

    private SonarQubeScanTaskManager() {
    }

    public static JSONObject start(JSONObject paramObj) {
        cleanExpiredTask();
        JSONObject sonarConfig = normalizeConfig(paramObj.getJSONObject("sonarConfig"));
        String taskId = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
        JSONObject task = new JSONObject();
        task.put("taskId", taskId);
        task.put("status", "running");
        task.put("success", false);
        long startTime = System.currentTimeMillis();
        task.put("startTime", startTime);
        task.put("timeout", sonarConfig.getIntValue("timeout"));
        TASK_MAP.put(taskId, task);
        SCAN_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject result = executeScanner(paramObj, sonarConfig);
                result.put("taskId", taskId);
                result.put("startTime", startTime);
                result.put("finishTime", System.currentTimeMillis());
                TASK_MAP.put(taskId, result);
            }
        });
        return task;
    }

    public static JSONObject getStatus(String taskId) {
        cleanExpiredTask();
        if (StringUtils.isBlank(taskId)) {
            throw new ApiRuntimeException("SonarQube扫描任务id不能为空");
        }
        JSONObject task = TASK_MAP.get(taskId);
        if (task == null) {
            JSONObject result = new JSONObject();
            result.put("taskId", taskId);
            result.put("status", "failed");
            result.put("success", false);
            result.put("message", "SonarQube扫描任务不存在或Runner已重启");
            return result;
        }
        return task;
    }

    private static JSONObject executeScanner(JSONObject paramObj, JSONObject sonarConfig) {
        JSONObject result = new JSONObject();
        result.put("status", "failed");
        result.put("success", false);
        GitWorkingCopy gitWorkingCopy = null;
        SVNWorkingCopy svnWorkingCopy = null;
        try {
            gitWorkingCopy = prepareGitWorkingCopy(paramObj, sonarConfig);
            svnWorkingCopy = prepareSvnWorkingCopy(paramObj, sonarConfig);
            File scanDirectory = resolveScanDirectory(paramObj, sonarConfig);
            result.put("scanDirectory", scanDirectory.getPath());
            if (!scanDirectory.exists() || !scanDirectory.isDirectory()) {
                result.put("message", "代码扫描目录不存在：" + scanDirectory.getPath());
                return result;
            }

            List<String> command = buildScannerCommand(sonarConfig);
            result.put("command", maskCommand(command, sonarConfig));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(scanDirectory);
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().putIfAbsent("SONAR_SCANNER_OPTS", "-Xss8m");

            Process process = processBuilder.start();
            ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
            Future<String> outputFuture = outputExecutor.submit(new ProcessOutputReader(process.getInputStream()));
            int timeout = Math.max(10, sonarConfig.getIntValue("timeout"));
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                outputExecutor.shutdownNow();
                result.put("message", "SonarQube扫描执行超时，超时时间：" + timeout + "秒");
                result.put("output", outputFuture.isDone() ? outputFuture.get() : "");
                return result;
            }
            String output = outputFuture.get(5, TimeUnit.SECONDS);
            outputExecutor.shutdownNow();
            int exitCode = process.exitValue();
            result.put("exitCode", exitCode);
            result.put("output", output);
            if (exitCode != 0) {
                result.put("message", "SonarQube扫描执行失败，退出码：" + exitCode);
                return result;
            }
            result.put("status", "completed");
            result.put("success", true);
            result.put("message", "SonarQube扫描执行成功");
            return result;
        } catch (Exception e) {
            result.put("message", StringUtils.defaultIfBlank(e.getMessage(), "SonarQube扫描执行异常"));
            return result;
        } finally {
            if (gitWorkingCopy != null) {
                gitWorkingCopy.unlock();
                gitWorkingCopy.close();
            }
            if (svnWorkingCopy != null) {
                svnWorkingCopy.unlock();
                svnWorkingCopy.close();
            }
        }
    }

    private static JSONObject normalizeConfig(JSONObject sonarConfig) {
        JSONObject config = sonarConfig == null ? new JSONObject() : new JSONObject(sonarConfig);
        config.put("scannerPath", StringUtils.defaultIfBlank(config.getString("scannerPath"), "sonar-scanner"));
        config.put("language", StringUtils.defaultIfBlank(config.getString("language"), "java"));
        config.put("encode", StringUtils.defaultIfBlank(config.getString("encode"), "UTF-8"));
        config.put("timeout", config.getInteger("timeout") == null ? DEFAULT_TIMEOUT_SECONDS : config.getInteger("timeout"));
        config.put("verbose", StringUtils.defaultIfBlank(config.getString("verbose"), "1"));
        config.put("src", StringUtils.defaultIfBlank(config.getString("src"), "."));
        config.put("bin", StringUtils.defaultIfBlank(config.getString("bin"), "."));
        return config;
    }

    private static void cleanExpiredTask() {
        long now = System.currentTimeMillis();
        for (String taskId : TASK_MAP.keySet()) {
            JSONObject task = TASK_MAP.get(taskId);
            Long startTime = task == null ? null : task.getLong("startTime");
            if (startTime != null && now - startTime > TASK_KEEP_MILLIS) {
                TASK_MAP.remove(taskId);
            }
        }
    }

    private static File resolveScanDirectory(JSONObject paramObj, JSONObject sonarConfig) {
        String workspacePath = sonarConfig.getString("workspacePath");
        if (StringUtils.isBlank(workspacePath)) {
            String branchName = sonarConfig.getString("branchName");
            if ("svn".equalsIgnoreCase(paramObj.getString("repoType")) && StringUtils.isNotBlank(branchName)) {
                workspacePath = WorkingCopyUtils.getBranchRealPath(paramObj, branchName);
            } else {
                workspacePath = WorkingCopyUtils.getWcPath(paramObj);
            }
        }
        File workspace = new File(workspacePath);
        String prefix = sonarConfig.getString("prefix");
        return StringUtils.isBlank(prefix) ? workspace : new File(workspace, prefix);
    }

    private static GitWorkingCopy prepareGitWorkingCopy(JSONObject paramObj, JSONObject sonarConfig) throws Exception {
        if (!"gitlab".equalsIgnoreCase(paramObj.getString("repoType"))) {
            return null;
        }
        if (StringUtils.isNotBlank(sonarConfig.getString("workspacePath"))) {
            return null;
        }
        String branchName = sonarConfig.getString("branchName");
        if (StringUtils.isBlank(branchName)) {
            return null;
        }
        GitWorkingCopy gitWorkingCopy = new GitWorkingCopy(
                WorkingCopyUtils.getWcPath(paramObj),
                paramObj.getString("url"),
                paramObj.getString("username"),
                paramObj.getString("password")
        );
        gitWorkingCopy.tryLock(Config.WORKING_COPY_TRYLOCK_WAIT_TIMEOUT, TimeUnit.SECONDS);
        gitWorkingCopy.resetAndUpdate();
        gitWorkingCopy.checkout(branchName, true);
        return gitWorkingCopy;
    }

    private static SVNWorkingCopy prepareSvnWorkingCopy(JSONObject paramObj, JSONObject sonarConfig) throws Exception {
        if (!"svn".equalsIgnoreCase(paramObj.getString("repoType"))) {
            return null;
        }
        if (StringUtils.isNotBlank(sonarConfig.getString("workspacePath"))) {
            return null;
        }
        String branchName = sonarConfig.getString("branchName");
        if (StringUtils.isBlank(branchName)) {
            return null;
        }
        SVNWorkingCopy svnWorkingCopy = new SVNWorkingCopy(
                WorkingCopyUtils.getBranchRealPath(paramObj, branchName),
                paramObj.getString("url"),
                StringUtils.defaultString(paramObj.getString("username")),
                StringUtils.defaultString(paramObj.getString("password")),
                StringUtils.defaultString(paramObj.getString("mainBranch")),
                StringUtils.defaultString(paramObj.getString("branchesPath")),
                StringUtils.defaultString(paramObj.getString("tagsPath"))
        );
        svnWorkingCopy.tryLock(Config.WORKING_COPY_TRYLOCK_WAIT_TIMEOUT, TimeUnit.SECONDS);
        svnWorkingCopy.checkout(branchName);
        return svnWorkingCopy;
    }

    private static List<String> buildScannerCommand(JSONObject sonarConfig) {
        List<String> command = new ArrayList<>();
        command.add(sonarConfig.getString("scannerPath"));
        command.add("-Dsonar.host.url=" + sonarConfig.getString("baseurl"));
        if (StringUtils.isNotBlank(sonarConfig.getString("user"))) {
            command.add("-Dsonar.login=" + sonarConfig.getString("user"));
        }
        if (StringUtils.isNotBlank(sonarConfig.getString("password"))) {
            command.add("-Dsonar.password=" + sonarConfig.getString("password"));
        }
        command.add("-Dsonar.projectKey=" + sonarConfig.getString("projectKey"));
        command.add("-Dsonar.projectName=" + sonarConfig.getString("projectName"));
        command.add("-Dsonar.projectVersion=" + sonarConfig.getString("projectVersion"));
        command.add("-Dsonar.language=" + sonarConfig.getString("language"));
        command.add("-Dsonar.sourceEncoding=" + sonarConfig.getString("encode"));
        command.add("-Dsonar.sources=" + sonarConfig.getString("src"));
        command.add("-Dsonar.java.binaries=" + sonarConfig.getString("bin"));
        command.add("-Dsonar.ws.timeout=" + sonarConfig.getIntValue("timeout"));
        if (StringUtils.isNotBlank(sonarConfig.getString("modules"))) {
            command.add("-Dsonar.modules=" + sonarConfig.getString("modules"));
        }
        if (sonarConfig.getIntValue("intervalDay") > 0) {
            command.add("-Dsonar.projectDate=" + LocalDate.now().minusDays(sonarConfig.getIntValue("intervalDay")));
        }
        if ("1".equals(sonarConfig.getString("verbose")) || sonarConfig.getBooleanValue("verbose")) {
            command.add("-Dsonar.verbose=true");
        }
        return command;
    }

    private static String maskCommand(List<String> command, JSONObject sonarConfig) {
        String password = sonarConfig.getString("password");
        StringBuilder builder = new StringBuilder();
        for (String item : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(StringUtils.isBlank(password) ? item : item.replace(password, "******"));
        }
        return builder.toString();
    }

    private static class ProcessOutputReader implements Callable<String> {
        private final InputStream inputStream;

        private ProcessOutputReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public String call() throws Exception {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
