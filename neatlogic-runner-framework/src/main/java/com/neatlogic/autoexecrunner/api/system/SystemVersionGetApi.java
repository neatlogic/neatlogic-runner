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
package com.neatlogic.autoexecrunner.api.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class SystemVersionGetApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(SystemVersionGetApi.class);

    private static final String DEFAULT_RUNNER_HOME = "/app/systems/neatlogic-runner";
    private static final String RUNNER_LIB_DIR = "lib";
    private static final String AUTOEXEC_SYSTEM_DIR = "autoexec";
    private static final String AUTOEXEC_BACKEND_VERSION_FILE = "version";

    @Override
    public String getName() {
        return "获取执行器系统版本信息";
    }

    @Description(desc = "获取执行器系统版本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray runnerJarList = getRunnerJarList();
        JSONObject autoexecBackend = getAutoexecBackendVersion();
        JSONArray tbodyList = new JSONArray();
        for (Object runnerJar : runnerJarList) {
            if (runnerJar instanceof JSONObject) {
                // tbodyList 和明细对象不能复用同一个 JSONObject，否则 fastjson 会序列化成 $ref，前端表格拿不到真实字段。
                tbodyList.add(copyVersionRow((JSONObject) runnerJar));
            }
        }
        tbodyList.add(copyVersionRow(autoexecBackend));
        resultObj.put("runnerJarList", runnerJarList);
        resultObj.put("autoexecBackend", autoexecBackend);
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    private JSONArray getRunnerJarList() {
        JSONArray runnerJarList = new JSONArray();
        String runnerLibPath = getRunnerLibPath();
        File libDir = new File(runnerLibPath);
        if (!libDir.exists() || !libDir.isDirectory()) {
            JSONObject row = newVersionRow("runnerJar", "neatlogic-runner");
            row.put("path", runnerLibPath);
            row.put("error", "runner lib directory does not exist");
            runnerJarList.add(row);
            return runnerJarList;
        }
        File[] jarFileArray = libDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (jarFileArray == null || jarFileArray.length == 0) {
            JSONObject row = newVersionRow("runnerJar", "neatlogic-runner");
            row.put("path", runnerLibPath);
            row.put("error", "runner jar file does not exist");
            runnerJarList.add(row);
            return runnerJarList;
        }
        Arrays.sort(jarFileArray, Comparator.comparing(File::getName));
        for (File jarFile : jarFileArray) {
            JSONObject row = getRunnerJarVersion(jarFile);
            if (row != null) {
                runnerJarList.add(row);
            }
        }
        return runnerJarList;
    }

    private JSONObject getRunnerJarVersion(File jarFile) {
        try (JarFile jf = new JarFile(jarFile)) {
            JarEntry pomPropertiesEntry = jf.stream()
                    // Maven 构建产物的版本信息统一落在 pom.properties，优先读取 neatlogic 自身 jar，过滤第三方依赖。
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith("/pom.properties"))
                    .filter(entry -> entry.getName().contains("neatlogic"))
                    .findFirst()
                    .orElse(null);
            if (pomPropertiesEntry == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(jf.getInputStream(pomPropertiesEntry));
            String groupId = properties.getProperty("groupId");
            String artifactId = properties.getProperty("artifactId");
            String version = properties.getProperty("version");
            if (!StringUtils.containsIgnoreCase(groupId, "neatlogic") && !StringUtils.containsIgnoreCase(artifactId, "neatlogic")) {
                return null;
            }
            JSONObject row = newVersionRow("runnerJar", StringUtils.defaultIfBlank(artifactId, jarFile.getName()));
            row.put("groupId", groupId);
            row.put("artifactId", artifactId);
            row.put("version", version);
            row.put("jarName", jarFile.getName());
            row.put("path", jarFile.getPath());
            setLastModified(row, jarFile.lastModified());
            return row;
        } catch (Exception ex) {
            logger.error("读取执行器jar版本信息失败: {}", jarFile.getPath(), ex);
            JSONObject row = newVersionRow("runnerJar", jarFile.getName());
            row.put("jarName", jarFile.getName());
            row.put("path", jarFile.getPath());
            row.put("error", ex.getMessage());
            setLastModified(row, jarFile.lastModified());
            return row;
        }
    }

    private JSONObject getAutoexecBackendVersion() {
        String autoexecBackendVersionPath = getAutoexecBackendVersionPath();
        File versionFile = new File(autoexecBackendVersionPath);
        JSONObject row = newVersionRow("autoexecBackend", "neatlogic-autoexec-backend");
        row.put("path", autoexecBackendVersionPath);
        if (!versionFile.exists() || !versionFile.isFile()) {
            row.put("error", "autoexec backend version file does not exist");
            return row;
        }
        setLastModified(row, versionFile.lastModified());
        try {
            String content = new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8).trim();
            row.put("version", getVersionFromContent(content));
        } catch (Exception ex) {
            logger.error("读取autoexec backend版本信息失败: {}", autoexecBackendVersionPath, ex);
            row.put("error", ex.getMessage());
        }
        return row;
    }

    private String getVersionFromContent(String content) throws IOException {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        if (content.startsWith("{") && content.endsWith("}")) {
            JSONObject versionObj = JSONObject.parseObject(content);
            String version = getFirstNotBlank(versionObj.getString("version"), versionObj.getString("VERSION"), versionObj.getString("buildVersion"), versionObj.getString("buildNo"));
            if (StringUtils.isNotBlank(version)) {
                return version;
            }
        }
        Properties properties = new Properties();
        properties.load(new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        String version = getFirstNotBlank(properties.getProperty("version"), properties.getProperty("VERSION"), properties.getProperty("build.version"), properties.getProperty("buildNo"), properties.getProperty("build.no"));
        if (StringUtils.isNotBlank(version)) {
            return version;
        }
        return content.split("\\R", 2)[0];
    }

    private String getFirstNotBlank(String... valueArray) {
        for (String value : valueArray) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private JSONObject newVersionRow(String type, String name) {
        JSONObject row = new JSONObject();
        row.put("type", type);
        row.put("name", name);
        return row;
    }

    private void setLastModified(JSONObject row, long lastModified) {
        row.put("lastModified", lastModified);
        row.put("lastModifiedText", TimeUtil.getTimeToDateString(lastModified, TimeUtil.YYYY_MM_DD_HH_MM_SS));
    }

    private JSONObject copyVersionRow(JSONObject row) {
        return JSONObject.parseObject(row.toJSONString());
    }

    private String getRunnerLibPath() {
        return new File(getRunnerHome(), RUNNER_LIB_DIR).getPath();
    }

    private String getAutoexecBackendVersionPath() {
        File runnerHome = new File(getRunnerHome());
        File systemsHome = runnerHome.getParentFile();
        if (systemsHome == null) {
            systemsHome = new File(DEFAULT_RUNNER_HOME).getParentFile();
        }
        return new File(new File(systemsHome, AUTOEXEC_SYSTEM_DIR), AUTOEXEC_BACKEND_VERSION_FILE).getPath();
    }

    private String getRunnerHome() {
        return StringUtils.defaultIfBlank(Config.getConfigProperty("runner.home", DEFAULT_RUNNER_HOME), DEFAULT_RUNNER_HOME);
    }

    @Override
    public String getToken() {
        return "/system/version/get";
    }
}
