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
import com.neatlogic.autoexecrunner.constvalue.JobAction;
import com.neatlogic.autoexecrunner.core.ExecProcessCommand;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.threadpool.CommonThreadPool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobDataPurgeApi extends PrivateApiComponentBase {
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
        commandList = Lists.newArrayList(commandList);
        if(commandVo.getPassThroughEnv() != null){
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        commandVo.setCommandList(commandList);
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/data/purge";
    }
}
