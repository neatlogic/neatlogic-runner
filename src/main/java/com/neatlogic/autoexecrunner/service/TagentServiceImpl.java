package com.neatlogic.autoexecrunner.service;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import com.neatlogic.autoexecrunner.common.tagent.IpUtil;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author lvzk
 * @since 2021/10/14 14:15
 **/
@Service
public class TagentServiceImpl implements TagentService {

    @Override
    public boolean forwardNeatlogicWeb(JSONObject jsonObj, String url, StringBuilder execInfo) {
        boolean status = false;
        if (jsonObj.containsKey("mgmtIp") && StringUtils.isNotBlank(jsonObj.getString("mgmtIp"))) {
            jsonObj.put("ip", jsonObj.containsKey("mgmtIp"));
        } else {
            jsonObj.put("ip", IpUtil.getIpAddr(UserContext.get().getRequest()));
        }
        RestVo restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), jsonObj);
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
                jsonObj.put("data", resultJson);
            } else {
                execInfo.append("Server Error,").append(httpResult);
            }
        } else {
            execInfo.append("neatlogic return message is blank");
        }
        return status;
    }
}
