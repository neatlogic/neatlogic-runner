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
import com.neatlogic.autoexecrunner.core.ExecProcessCommand;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;
import com.neatlogic.autoexecrunner.threadpool.CommonThreadPool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobAbortApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "中止作业剧本进程";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CommandVo commandVo = new CommandVo(jsonObj);
        commandVo.setAction(JobAction.ABORT.getValue());
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--abort");
        commandList = new ArrayList<>(commandList);
        if (commandVo.getPassThroughEnv() != null) {
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        commandVo.setCommandList(commandList);
        //队列里不存在才执行abort命令
        if (!AutoexecQueueThread.removeCommand(commandVo)) {
            ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
            CommonThreadPool.execute(processCommand);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/abort";
    }
}
