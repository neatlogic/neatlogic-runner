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
package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Component
public class GetJobWaitingDetailApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取作业排队状态";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "groupSort", type = ApiParamType.LONG, desc = "阶段组id"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String jobId = jsonObj.getString("jobId");
        Integer groupSort = jsonObj.getInteger("groupSort");
        List<CommandVo> list = AutoexecQueueThread.getBlockingQueueByJobIdAndGroupSort();
        JSONObject result = new JSONObject();
        for (int i = 0; i < list.size(); i++) {
            CommandVo commandVo = list.get(i);
            JSONObject commandJson = new JSONObject();
            commandJson.put("groupSortList",commandVo.getJobGroupSortList());
            commandJson.put("nodeSqlList",commandVo.getJobPhaseNodeSqlList());
            commandJson.put("phaseNameList",commandVo.getJobPhaseNameList());
            commandJson.put("resourceIdList",commandVo.getJobPhaseResourceIdList());
            commandJson.put("fcd",commandVo.getFcd().getTime());
            commandJson.put("command",commandVo.getCommandList().stream().map(Object::toString).collect(Collectors.joining("','")));
            if (Objects.equals(commandVo.getJobId(), jobId) && (groupSort == null || commandVo.getJobGroupSortList().contains(groupSort))) {
                result.put(String.valueOf(i + 1), commandJson);
            }
        }
        result.put("count", AutoexecQueueThread.getBlockingQueueSize());
        return result;
    }

    @Override
    public String getToken() {
        return "/job/waiting/detail/get";
    }
}
