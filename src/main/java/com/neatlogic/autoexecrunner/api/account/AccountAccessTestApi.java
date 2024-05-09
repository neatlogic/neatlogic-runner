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
