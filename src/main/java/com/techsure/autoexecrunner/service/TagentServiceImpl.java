package com.techsure.autoexecrunner.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.TagentConfig;
import com.techsure.autoexecrunner.common.tagent.IpUtil;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author lvzk
 * @since 2021/10/14 14:15
 **/
@Service
public class TagentServiceImpl implements TagentService{

    @Override
    public boolean forwardCodedriverWeb(JSONObject jsonObj, String url, StringBuilder execInfo) {
        boolean status = false;
        jsonObj.put("ip", IpUtil.getIpAddr(UserContext.get().getRequest()));
        Map<String, String> params = (Map<String, String>) JSON.parse(jsonObj.toJSONString());
        RestVo restVo = new RestVo( url, AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));
        restVo.setTenant(jsonObj.getString("tenant"));
        restVo.setAuthType(TagentConfig.AUTH_TYPE);
        restVo.setUsername(TagentConfig.ACCESS_KEY);
        restVo.setPassword(TagentConfig.ACCESS_SECRET);
        String httpResult = RestUtil.sendRequest(restVo);
        JSONObject resultJson = JSONObject.parseObject(httpResult);
        if (StringUtils.isNotBlank(httpResult)) {
            if ("OK".equals(resultJson.getString("Status"))) {
                String httpStatus = resultJson.getJSONObject("Return").getString("Status");
                if ("OK".equals(httpStatus)) {
                    status = true;
                    jsonObj.put("data",resultJson.getJSONObject("Return").getJSONObject("Data"));
                } else {
                    execInfo.append("Server Error,").append(resultJson.toString());
                }
            } else {
                execInfo.append("call codedriver failed,http return error,").append(resultJson.toString());
            }
        } else {
            execInfo.append("codedriver return message is blank");
        }
        return status;
    }
}
