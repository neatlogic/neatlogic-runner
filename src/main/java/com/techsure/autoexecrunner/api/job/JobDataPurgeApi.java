/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.techsure.autoexecrunner.constvalue.JobAction;
import com.techsure.autoexecrunner.core.ExecProcessCommand;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.threadpool.CommonThreadPool;
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
