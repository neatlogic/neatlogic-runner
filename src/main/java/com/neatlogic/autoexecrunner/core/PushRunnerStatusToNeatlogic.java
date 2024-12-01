package com.neatlogic.autoexecrunner.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.api.tagent.TagentRegisterApi;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.dto.TenantVo;
import com.neatlogic.autoexecrunner.dto.UserVo;
import com.neatlogic.autoexecrunner.filter.core.LoginAuthHandlerBase;
import com.neatlogic.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class PushRunnerStatusToNeatlogic {
    private static final Logger logger = LoggerFactory.getLogger(PushRunnerStatusToNeatlogic.class);

    @PostConstruct
    public final void init() {
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<TenantVo> tenantVoList;
                    JSONObject param = new JSONObject();
                    String urlTenant = String.format("%s/tenant/get/active/tenant/list", Config.NEATLOGIC_ROOT());
                    RestVo restTenant = new RestVo(urlTenant, param, AuthenticateType.NOAUTH.getValue(), param.getString("tenant"));
                    String httpResultTenant = RestUtil.sendRequest(restTenant);
                    if (StringUtils.isNotBlank(httpResultTenant)) {
                        JSONObject resultJsonTenant = JSON.parseObject(httpResultTenant);
                        String httpStatusTenant = resultJsonTenant.getString("Status");
                        if ("OK".equals(httpStatusTenant)) {
                            param.put("tagentRegisterCount", TagentRegisterApi.getCount());
                            tenantVoList = resultJsonTenant.getJSONArray("tenantList").toJavaList(TenantVo.class);
                            for (TenantVo tenantVo : tenantVoList) {
                                String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_RUNNER_STATUS);
                                RestVo restVo = new RestVo(url, param, AuthenticateType.HMAC.getValue(), tenantVo.getUuid());
                                UserVo userVo = SystemUser.SYSTEM.getUserVo();
                                LoginAuthHandlerBase.buildJwt(userVo);
                                restVo.setToken(userVo.getAuthorization());
                                String httpResult = RestUtil.sendRequest(restVo);
                                if (StringUtils.isNotBlank(httpResult)) {
                                    JSONObject resultJson = JSON.parseObject(httpResult);
                                    String httpStatus = resultJson.getString("Status");
                                    if (!"OK".equals(httpStatus)) {
                                        logger.error("pushRunnerStatusToNeatlogic failed!");
                                    }
                                } else {
                                    logger.error("pushRunnerStatusToNeatlogic failed!");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, 0, 1000); // 半个小时执行一次
    }
}
