/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import org.springframework.stereotype.Component;

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
