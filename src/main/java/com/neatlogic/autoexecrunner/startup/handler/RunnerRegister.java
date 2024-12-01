package com.neatlogic.autoexecrunner.startup.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.dto.TenantVo;
import com.neatlogic.autoexecrunner.startup.IStartUp;
import com.neatlogic.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class RunnerRegister implements IStartUp {
    @Override
    public String getName() {
        return "runnerRegister";
    }

    @Override
    public String getDescription() {
        return "启动时自动注册runner";
    }

    @Override
    public void doService() {
        List<TenantVo> tenantVoList;
        JSONObject param = new JSONObject();
        String urlTenant = String.format("%s/tenant/get/active/tenant/list", Config.NEATLOGIC_ROOT());
        RestVo restTenant = new RestVo(urlTenant, param, AuthenticateType.NOAUTH.getValue(), param.getString("tenant"));
        String httpResultTenant = RestUtil.sendRequest(restTenant);
        if (StringUtils.isNotBlank(httpResultTenant)) {
            JSONObject resultJsonTenant = JSON.parseObject(httpResultTenant);
            String httpStatusTenant = resultJsonTenant.getString("Status");
            if ("OK".equals(httpStatusTenant)) {
                JSONObject params = new JSONObject();
                params.put("nettyPort", TagentConfig.AUTOEXEC_NETTY_PORT);
                params.put("port", Config.SERVER_PORT());
                params.put("protocol", Boolean.TRUE.equals(Config.IS_SSL()) ? "https" : "http");
                tenantVoList = resultJsonTenant.getJSONArray("tenantList").toJavaList(TenantVo.class);
                for (TenantVo tenantVo : tenantVoList) {
                    String url = String.format("%s/any/api/t/%s/rest/runner/register", Config.NEATLOGIC_ROOT(), tenantVo.getUuid());
                    RestUtil.sendRequest(new RestVo(url, params, AuthenticateType.HMAC.getValue(), TenantContext.get().getTenantUuid()));
                }
            }
        }
    }
}
