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
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.constvalue.JobAction;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.exception.job.JobQueueFullException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/21 17:31
 **/
@Component
public class JobExecApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(JobExecApi.class);

    @Override
    public String getName() {
        return "创建执行作业剧本进程";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CommandVo commandVo = new CommandVo(jsonObj);
        commandVo.setAction(JobAction.EXEC.getValue());
        //save params.json
        /*String filePath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(commandVo.getJobId(), new StringBuilder()) + File.separator + "params.json";
        FileUtil.saveFile(commandVo.getConfig(), filePath);*/
        //set command
        List<String> commandList = Arrays.asList("autoexec",
                "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--execid", commandVo.getExecid()
        );//--paramsfile 参数 仅用于测试
        commandList = new ArrayList<>(commandList);
        if (commandVo.getFirstFire() != null && commandVo.getFirstFire()) {
            commandList.add("--firstfire");
            //删除当前consoleLog
            FileUtil.deleteDirectoryOrFile(commandVo.getConsoleLogPath());
        }
        if (commandVo.getNoFireNext() != null && commandVo.getNoFireNext()) {
            commandList.add("--nofirenext");
        }
        if (commandVo.getPassThroughEnv() != null) {
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        if (CollectionUtils.isNotEmpty(commandVo.getJobGroupSortList())) {
            commandList.add("--phasegroups");
            commandList.add(commandVo.getJobGroupSortList().stream().map(Object::toString).collect(Collectors.joining("','")));
        }
        if (CollectionUtils.isNotEmpty(commandVo.getJobPhaseNameList())) {
            commandList.add("--phases");
            commandList.add(commandVo.getJobPhaseNameList().stream().map(Object::toString).collect(Collectors.joining("','")));
        }
        if (CollectionUtils.isNotEmpty(commandVo.getJobPhaseResourceIdList())) {
            commandList.add("--nodes");
            commandList.add(commandVo.getJobPhaseResourceIdList().stream().map(Object::toString).collect(Collectors.joining("','")));
        }
        if (CollectionUtils.isNotEmpty(commandVo.getJobPhaseNodeSqlList())) {
            commandList.add("--sqlfiles");
            commandList.add(commandVo.getJobPhaseNodeSqlList().toString());
        }
        commandList.add("--reuseconslog");
        commandVo.setCommandList(commandList);

        int addResult = AutoexecQueueThread.addCommand(commandVo);
        if (addResult == -1) {
            throw new JobQueueFullException();
        } else if (addResult == 0) {
            logger.debug("队列里已存在相同的执行命令：{}", String.join(",", commandList));
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/exec";
    }
}
