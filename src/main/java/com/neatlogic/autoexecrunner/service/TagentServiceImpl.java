package com.neatlogic.autoexecrunner.service;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.tagent.IpUtil;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/10/14 14:15
 **/
@Service
public class TagentServiceImpl implements TagentService {

    @Override
    public boolean forwardNeatlogicWeb(JSONObject jsonObj, String url, StringBuilder execInfo) throws Exception {
        boolean status = false;
        if (jsonObj.containsKey("mgmtIp") && StringUtils.isNotBlank(jsonObj.getString("mgmtIp"))) {
            jsonObj.put("ip", jsonObj.getString("mgmtIp"));
        } else {
            jsonObj.put("ip", IpUtil.getIpAddr(UserContext.get().getRequest()));
        }
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setPayload(jsonObj.toJSONString())
                .setAuthType(AuthenticateType.HMAC)
                .setTenant(jsonObj.getString("tenant"))
                .setToken(SystemUser.AUTOEXEC.getToken())
                .setUsername(SystemUser.AUTOEXEC.getUserId())
                .sendRequest();
        if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
            throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s",
                    url, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
        }

        JSONObject resultJson = httpRequestUtil.getResultJson();

        if (MapUtils.isNotEmpty(resultJson)) {
            String httpStatus = resultJson.getString("Status");
            if ("OK".equals(httpStatus)) {
                status = true;
                jsonObj.put("data", resultJson);
            } else {
                execInfo.append("Server Error,").append(httpRequestUtil.getResult());
            }
        } else {
            execInfo.append("neatlogic return message is blank");
        }
        return status;
    }
}
