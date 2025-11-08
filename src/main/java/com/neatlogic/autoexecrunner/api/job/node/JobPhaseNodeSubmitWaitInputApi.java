/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
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
