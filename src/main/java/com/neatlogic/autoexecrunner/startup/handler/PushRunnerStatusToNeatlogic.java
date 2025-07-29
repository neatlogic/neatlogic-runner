package com.neatlogic.autoexecrunner.startup.handler;

import com.neatlogic.autoexecrunner.startup.IStartUp;

public class PushRunnerStatusToNeatlogic implements IStartUp {
    //private static final Logger logger = LoggerFactory.getLogger(PushRunnerStatusToNeatlogic.class);
    @Override
    public String getName() {
        return "pushRunnerStatus";
    }

    @Override
    public String getDescription() {
        return "启动时定时推送runner状态";
    }

    @Override
    public void doService() {
        //neatlogic端IP可能会获取不准确 先屏蔽
//        Timer timer = new Timer();
//
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    List<TenantVo> tenantVoList;
//                    JSONObject param = new JSONObject();
//                    String urlTenant = String.format("%s/tenant/get/active/tenant/list", Config.NEATLOGIC_ROOT());
//                    HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(urlTenant).setPayload(param.toJSONString()).setAuthType(AuthenticateType.NOAUTH).sendRequest();
//                    if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
//                        throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s",
//                                urlTenant, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
//                    }
//                    String httpResultTenant = httpRequestUtil.getResult();
//                    if (StringUtils.isNotBlank(httpResultTenant)) {
//                        JSONObject resultJsonTenant = JSON.parseObject(httpResultTenant);
//                        String httpStatusTenant = resultJsonTenant.getString("Status");
//                        if ("OK".equals(httpStatusTenant)) {
//                            param.put("tagentRegisterCount", TagentRegisterApi.getCount());
//                            tenantVoList = resultJsonTenant.getJSONArray("tenantList").toJavaList(TenantVo.class);
//                            for (TenantVo tenantVo : tenantVoList) {
//                                String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_RUNNER_STATUS);
//                                RestVo restVo = new RestVo(url, param, AuthenticateType.HMAC.getValue(), tenantVo.getUuid());
//                                UserVo userVo = SystemUser.SYSTEM.getUserVo();
//                                LoginAuthHandlerBase.buildJwt(userVo);
//                                restVo.setToken(userVo.getAuthorization());
//                                String httpResult = RestUtil.sendRequest(restVo);
//                                if (StringUtils.isNotBlank(httpResult)) {
//                                    JSONObject resultJson = JSON.parseObject(httpResult);
//                                    String httpStatus = resultJson.getString("Status");
//                                    if (!"OK".equals(httpStatus)) {
//                                        logger.error("pushRunnerStatusToNeatlogic failed!");
//                                    }
//                                } else {
//                                    logger.error("pushRunnerStatusToNeatlogic failed!");
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    logger.error(e.getMessage(), e);
//                }
//            }
//        }, 0, Config.UPDATE_RUNNER_STATUS_PERIOD());
    }
}
