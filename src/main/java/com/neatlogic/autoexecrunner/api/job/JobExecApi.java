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
import com.neatlogic.autoexecrunner.util.FileUtil;
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
                "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--execid", commandVo.getExecid()
        );//--paramsfile 参数 仅用于测试
        commandList = Lists.newArrayList(commandList);
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
        commandList.add("--reuseconslog");
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
