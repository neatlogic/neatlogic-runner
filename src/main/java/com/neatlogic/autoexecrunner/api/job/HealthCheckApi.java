/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
