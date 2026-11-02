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
package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.JobAction;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 同步执行历史作业清理，并处理子进程输出。
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobDataPurgeApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(JobDataPurgeApi.class);
    private static final File NULL_FILE = new File("/dev/null");

    @Override
    public String getName() {
        return "清除历史作业";
    }

    /**
     * 丢弃标准输出，运行期间读取错误输出，避免管道写满导致清理进程无法退出。
     */
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CommandVo commandVo = new CommandVo(jsonObj);
        commandVo.setAction(JobAction.PURGE.getValue());
        // 组装清理命令
        List<String> commandList = Arrays.asList("autoexec", "--purgejobdata", jsonObj.getString("expiredDays"));
        commandList = new ArrayList<>(commandList);
//        if (commandVo.getPassThroughEnv() != null) {
//            commandList.add("--passthroughenv");
//            commandList.add(commandVo.getPassThroughEnv().toString());
//        }

        ProcessBuilder builder = new ProcessBuilder(commandList);
        // 不保留逐目录清理日志，避免日志持续增长或标准输出管道写满。
        builder.redirectOutput(NULL_FILE);
        Process proc = builder.start();
        StringWriter writer = new StringWriter();
        try {
            // 清理命令无需标准输入，及时关闭以传递 EOF。
            proc.getOutputStream().close();
            try (InputStreamReader reader = new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8)) {
                // 必须先消费错误输出，再等待退出；否则 stderr 同样可能写满管道。
                IOUtils.copy(reader, writer);
            }
            proc.waitFor();
        } finally {
            // 读取异常或等待中断时终止子进程，避免留下无人管理的清理进程。
            if (proc.isAlive()) {
                proc.destroy();
            }
        }
        if (StringUtils.isNotBlank(writer.toString())) {
            logger.error(writer.toString());
            throw new ApiRuntimeException(writer.toString());
        }

        return null;
    }

    @Override
    public String getToken() {
        return "/job/data/purge";
    }
}
