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
    public boolean forwardNeatlogicWeb(JSONObject jsonObj, String url, StringBuilder execInfo, boolean isFromTagent) {
        boolean status = false;
        if (isFromTagent) {
            jsonObj.put("ip", IpUtil.getIpAddr(UserContext.get().getRequest()));
        }
        Map<String, String> params = (Map<String, String>) JSON.parse(jsonObj.toJSONString());
        RestVo restVo = new RestVo( url, AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));
        restVo.setTenant(jsonObj.getString("tenant"));
        restVo.setAuthType(TagentConfig.AUTH_TYPE);
        restVo.setUsername(TagentConfig.ACCESS_KEY);
        restVo.setPassword(TagentConfig.ACCESS_SECRET);
        String httpResult = RestUtil.sendRequest(restVo);
        if (StringUtils.isNotBlank(httpResult)) {
            JSONObject resultJson = JSONObject.parseObject(httpResult);
            String httpStatus = resultJson.getString("Status");
            if ("OK".equals(httpStatus)) {
                status = true;
                jsonObj.put("data",resultJson);
            } else {
                execInfo.append("Server Error,").append(httpResult);
            }
        } else {
            execInfo.append("neatlogic return message is blank");
        }
        return status;
    }
}
