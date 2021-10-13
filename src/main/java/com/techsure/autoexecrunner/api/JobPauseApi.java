package com.techsure.autoexecrunner.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.core.ExecManager;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

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
        ExecManager.pause(commandVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/pause";
    }
}