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
import com.neatlogic.autoexecrunner.core.ExecProcessCommand;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobDataPurgeApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(ExecProcessCommand.class);

    @Override
    public String getName() {
        return "清除历史作业";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CommandVo commandVo = new CommandVo(jsonObj);
        commandVo.setAction(JobAction.PURGE.getValue());
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--purgejobdata", jsonObj.getString("expiredDays"));
        commandList = new ArrayList<>(commandList);
//        if (commandVo.getPassThroughEnv() != null) {
//            commandList.add("--passthroughenv");
//            commandList.add(commandVo.getPassThroughEnv().toString());
//        }

        ProcessBuilder builder = new ProcessBuilder(commandList);
        Process proc = builder.start();
        proc.waitFor();
        DataInputStream input = new DataInputStream(proc.getErrorStream());
        StringWriter writer = new StringWriter();
        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        IOUtils.copy(reader, writer);
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
