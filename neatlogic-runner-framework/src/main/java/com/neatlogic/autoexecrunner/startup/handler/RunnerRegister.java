package com.neatlogic.autoexecrunner.startup.handler;

import com.neatlogic.autoexecrunner.startup.IStartUp;

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
        //neatlogic端IP可能会获取不准确 先屏蔽
//        List<TenantVo> tenantVoList;
//        JSONObject param = new JSONObject();
//        String urlTenant = String.format("%s/tenant/get/active/tenant/list", Config.NEATLOGIC_ROOT());
//        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(urlTenant).setPayload(param.toJSONString()).setAuthType(AuthenticateType.NOAUTH).sendRequest();
//        if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
//            throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s",
//                    urlTenant, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
//        }
//        String httpResultTenant = httpRequestUtil.getResult();
//        if (StringUtils.isNotBlank(httpResultTenant)) {
//            JSONObject resultJsonTenant = JSON.parseObject(httpResultTenant);
//            String httpStatusTenant = resultJsonTenant.getString("Status");
//            if ("OK".equals(httpStatusTenant)) {
//                JSONObject params = new JSONObject();
//                params.put("nettyPort", TagentConfig.AUTOEXEC_NETTY_PORT);
//                params.put("port", Config.SERVER_PORT());
//                params.put("protocol", Boolean.TRUE.equals(Config.IS_SSL()) ? "https" : "http");
//                tenantVoList = resultJsonTenant.getJSONArray("tenantList").toJavaList(TenantVo.class);
//                for (TenantVo tenantVo : tenantVoList) {
//                    String url = String.format("%s/any/api/t/%s/rest/runner/register", Config.NEATLOGIC_ROOT(), tenantVo.getUuid());
//                    httpRequestUtil = HttpRequestUtil.post(url).setPayload(params.toJSONString())
//                            .setAuthType(AuthenticateType.HMAC)
//                            .setTenant(TenantContext.get().getTenantUuid())
//                            .setToken(SystemUser.AUTOEXEC.getToken())
//                            .setUsername(SystemUser.AUTOEXEC.getUserId())
//                            .sendRequest();
//                    if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
//                        throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s",
//                                url, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
//                    }
//                }
//            }
//        }
    }
}
