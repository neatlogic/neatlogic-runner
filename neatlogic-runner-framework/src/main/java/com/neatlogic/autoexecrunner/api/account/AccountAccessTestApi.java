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
package com.neatlogic.autoexecrunner.api.account;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountAccessTestApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(AccountAccessTestApi.class);

    @Override
    public String getToken() {
        return "/account/accesstest";
    }

    @Override
    public String getName() {
        return "测试账号可用性";
    }

    @Input({
            @Param(name = "accountList", type = ApiParamType.JSONARRAY, desc = "账号列表", isRequired = true),
            @Param(name = "accountList.host", type = ApiParamType.STRING, desc = "IP"),
            @Param(name = "accountList.port", type = ApiParamType.STRING, desc = "端口"),
            @Param(name = "accountList.protocolPort", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "accountList.protocol", type = ApiParamType.STRING, desc = "协议"),
            @Param(name = "accountList.nodeName", type = ApiParamType.STRING, desc = "节点名称"),
            @Param(name = "accountList.nodeType", type = ApiParamType.STRING, desc = "节点操作系统类型"),
            @Param(name = "accountList.username", type = ApiParamType.STRING, desc = "用户名"),
            @Param(name = "accountList.password", type = ApiParamType.STRING, desc = "密码"),
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray result = new JSONArray();
        JSONArray accountList = jsonObj.getJSONArray("accountList");
        for (int i = 0; i < accountList.size(); i++) {
            JSONObject object = accountList.getJSONObject(i);
            try {
                ProcessBuilder builder = new ProcessBuilder("nodeconntest", "--node", object.toJSONString());
                Process proc = builder.start();
                proc.waitFor();
                String msgError = IOUtils.toString(proc.getErrorStream());
                logger.error(msgError);
                object.put("msgError", msgError);
                object.put("exitValue", proc.exitValue());
                object.remove("password");
            } catch (Exception ex) {
                object.put("exitValue", 1);
                object.put("msgError", ex.getMessage());
                logger.error(ex.getMessage(), ex);
            }
            result.add(object);
        }
        return result;
    }

}
