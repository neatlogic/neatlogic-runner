package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.TagentConfig;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.common.tagent.IpUtil;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.publicapi.PublicApiComponentBase;
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TagentStatusUpdateApi extends PublicApiComponentBase {
    @Override
    public String getName() {
        return "tagent状态更新接口";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
            @Param(name = "credential", type = ApiParamType.STRING, desc = "tagent 密码", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "tagent状态更新")
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
            restVo = new RestVo(TagentConfig.AUTOEXEC_CODEDRIVER_ROOT + "/" + Constant.ACTION_UPDATE_TAGENT, AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));
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


    @Override
    public String getToken() {
        return "tagent/status/update";
    }
}
