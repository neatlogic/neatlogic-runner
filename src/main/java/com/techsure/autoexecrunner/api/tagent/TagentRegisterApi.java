package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.TagentConfig;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.common.tagent.IpUtil;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.restful.core.publicapi.PublicApiComponentBase;
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TagentRegisterApi extends PublicApiComponentBase {
    @Override
    public String getName() {
        return "tagent注册接口";
    }

    @Override
    public String getToken() {
        return "tagent/register";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean status = false;
        JSONObject data = new JSONObject();
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        try {
            jsonObj.put("ip", IpUtil.getIpAddr(UserContext.get().getRequest()));
            Map<String, String> params = (Map<String, String>) JSON.parse(String.valueOf(jsonObj));
            String httpResult = StringUtils.EMPTY;
            JSONObject resultJson = null;
            RestVo restVo = null;
            restVo = new RestVo(TagentConfig.AUTOEXEC_CODEDRIVER_ROOT + "/" + Constant.ACTION_REGISTER_TAGENT, AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));
            restVo.setTenant(jsonObj.getString("tenant"));
            restVo.setAuthType(TagentConfig.AUTH_TYPE);
            restVo.setUsername(TagentConfig.ACCESS_KEY);
            restVo.setPassword(TagentConfig.ACCESS_SECRET);
            httpResult = RestUtil.sendRequest(restVo);
            resultJson = JSONObject.parseObject(httpResult);
            if (StringUtils.isNotBlank(httpResult)) {
                if ("OK".equals(resultJson.getString("Status"))) {
                    String httpStatus = resultJson.getJSONObject("Return").getString("Status");
                    if ("OK".equals(httpStatus)) {
                        status = true;
                        data = resultJson.getJSONObject("Return").getJSONObject("Data");
                    } else {
                        execInfo.append("Server Error,").append(resultJson.toString());
                    }
                } else {
                    execInfo.append("call codedriver failed,http return error,").append(resultJson.toString());
                }
            } else {
                execInfo.append("codedriver return message is blank");
            }

        } catch (Exception ex) {
            status = false;
            execInfo.append("runner exec error :").append(ExceptionUtils.getStackTrace(ex));
        }

        if (status) {
            return data;
        } else {
            result.put("Message", execInfo);
            return result;
        }
    }


}
