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
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

/**
 * @author chenqiwei
 * @since 2021/2/1010:54 上午
 **/
@Component
public class HealthCheckApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "健康检查";
    }

    @Description(desc = "健康检查接口")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return null;
    }

    @Override
    public String getToken() {
        return "/health/check";
    }
}
