/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
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
 * @since 2021/6/17 19:31
 **/
@Component
public class JobPauseApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "暂停作业剧本进程";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CommandVo commandVo = new CommandVo(jsonObj);
        commandVo.setAction(JobAction.PAUSE.getValue());
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--pause");
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
        return "/job/pause";
    }
}
