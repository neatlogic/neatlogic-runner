package com.neatlogic.autoexecrunner.startup.handler;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.startup.IStartUp;
import com.neatlogic.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;

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
        JSONObject params = new JSONObject();
        params.put("nettyPort", TagentConfig.AUTOEXEC_NETTY_PORT);
        params.put("port", Config.SERVER_PORT());
        params.put("protocol", Config.IS_SSL() ? "https" : "http");
        String registerTenants = Config.REGISTER_TENANTS();
        if (StringUtils.isNotBlank(registerTenants)) {
            String[] tenants = registerTenants.split(",");
            for (String tenant : tenants) {
                String url = String.format("%s/any/api/t/%s/rest/runner/register", Config.NEATLOGIC_ROOT(), tenant);
                RestUtil.sendRequest(new RestVo(url, params, AuthenticateType.BEARER.getValue(), TenantContext.get().getTenantUuid()));
            }
        }
    }
}
