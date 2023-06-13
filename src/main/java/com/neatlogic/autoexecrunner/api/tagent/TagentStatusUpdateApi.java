package com.neatlogic.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.service.TagentService;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.restful.core.publicapi.PublicApiComponentBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
            status = tagentService.forwardNeatlogicWeb(jsonObj, String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_UPDATE_TAGENT), execInfo,true);
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
