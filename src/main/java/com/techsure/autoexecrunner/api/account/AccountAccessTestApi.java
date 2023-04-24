/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.account;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
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
                logger.error(IOUtils.toString(proc.getErrorStream()));
                object.put("exitValue", proc.exitValue());
                object.remove("password");
            } catch (Exception ex) {
                object.put("exitValue", 1);
                logger.error(ex.getMessage(), ex);
            }
            result.add(object);
        }
        return result;
    }

}
