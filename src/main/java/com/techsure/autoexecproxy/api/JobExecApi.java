package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
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
        //TODO 执行命令
        CommandVo commandVo = new CommandVo(jsonObj);
        ExecManager.exec(commandVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/exec";
    }
}
