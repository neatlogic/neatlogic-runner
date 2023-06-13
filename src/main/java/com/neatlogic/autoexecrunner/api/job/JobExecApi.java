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
package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.constvalue.JobAction;
import com.neatlogic.autoexecrunner.core.ExecProcessCommand;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.threadpool.CommonThreadPool;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/4/21 17:31
 **/
@Component
public class JobExecApi extends PrivateApiComponentBase {
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
                "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid()
        );//--paramsfile 参数 仅用于测试
        commandList = Lists.newArrayList(commandList);
        if (commandVo.getFirstFire() != null && commandVo.getFirstFire()) {
            commandList.add("--firstfire");
        }
        if (commandVo.getNoFireNext() != null && commandVo.getNoFireNext()) {
            commandList.add("--nofirenext");
        }
        if (commandVo.getPassThroughEnv() != null) {
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        if (CollectionUtils.isNotEmpty(commandVo.getJobGroupIdList())) {
            commandList.add("--phasegroups");
            commandList.add(commandVo.getJobGroupIdList().stream().map(Object::toString).collect(Collectors.joining("','")));
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
        commandVo.setCommandList(commandList);
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/exec";
    }
}
