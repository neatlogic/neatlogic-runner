package com.techsure.autoexecrunner.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.core.ExecManager;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

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
        ExecManager.exec(commandVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/exec";
    }
}
