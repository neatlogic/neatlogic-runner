package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.Config;
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
import com.techsure.autoexecrunner.service.TagentService;
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class TagentStatusUpdateApi extends PublicApiComponentBase {
    @Resource
    TagentService tagentService;

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
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        try {
            status = tagentService.forwardCodedriverWeb(jsonObj, String.format("%s/api/rest/%s", Config.CODEDRIVER_ROOT(), Constant.ACTION_UPDATE_TAGENT), execInfo);
        } catch (Exception ex) {
            execInfo.append("runner exec error :").append(ExceptionUtils.getStackTrace(ex));
        }
        if (status) {
            return jsonObj.get("data");
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
