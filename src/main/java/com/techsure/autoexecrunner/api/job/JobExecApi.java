/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.JobAction;
import com.techsure.autoexecrunner.util.JobUtil;
import com.techsure.autoexecrunner.core.ExecProcessCommand;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.threadpool.CommonThreadPool;
import com.techsure.autoexecrunner.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
        String filePath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(commandVo.getJobId(), new StringBuilder()) + File.separator + "params.json";
        FileUtil.saveFile(commandVo.getConfig(), filePath);
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--paramsfile", filePath);
        commandList = Lists.newArrayList(commandList);
        if (commandVo.getFirstFire() != null && commandVo.getFirstFire()) {
            commandList.add("--firstfire");
        }
        if (commandVo.getNoFireNext() != null && commandVo.getNoFireNext()) {
            commandList.add("--nofirenext");
        }
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
        return "/job/exec";
    }
}
