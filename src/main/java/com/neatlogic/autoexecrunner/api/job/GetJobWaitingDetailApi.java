/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
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
        List<CommandVo> list = AutoexecQueueThread.getBlockingQueueByJobIdAndGroupSort(jobId,groupSort);
        JSONObject result = new JSONObject();
        for (int i = 0; i < list.size(); i++) {
            CommandVo commandVo = list.get(i);
            JSONObject commandJson = new JSONObject();
            commandJson.put("fcd",commandVo.getFcd().getTime());
            commandJson.put("command",commandVo.getCommandList().stream().map(Object::toString).collect(Collectors.joining("','")));
            result.put(String.valueOf(i + 1), commandJson);
        }
        result.put("count", list.size());
        return result;
    }

    @Override
    public String getToken() {
        return "/job/waiting/detail/get";
    }
}
