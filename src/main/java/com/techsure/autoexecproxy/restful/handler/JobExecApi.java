package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

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
        commandVo.setCommandList(Collections.singletonList("ipconfig"));
        ExecManager.exec(commandVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/exec";
    }
}
