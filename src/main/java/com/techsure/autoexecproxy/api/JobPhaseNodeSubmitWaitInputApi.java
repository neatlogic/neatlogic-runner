package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/8/17 14:31
 **/
@Component
public class JobPhaseNodeSubmitWaitInputApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "提交作业节点waitInput";
    }

    @Input({
            @Param(name = "option", type = ApiParamType.STRING, desc = "waitInput 选项", isRequired = true),
            @Param(name = "pipeFile", type = ApiParamType.STRING, desc = "waitInput 管道文件路径", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String option = jsonObj.getString("option");
        String pipeFile = jsonObj.getString("pipeFile");
        //将option 写入管道
        FileUtil.saveFile(option,pipeFile);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/submit/waitInput";
    }
}
