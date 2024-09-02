package com.neatlogic.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.service.TagentService;
import com.neatlogic.autoexecrunner.restful.core.publicapi.PublicApiComponentBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TagentRegisterApi extends PublicApiComponentBase {
    @Resource
    TagentService tagentService;

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
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        try {
            String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_REGISTER_TAGENT);
            status = tagentService.forwardNeatlogicWeb(jsonObj,url,execInfo);
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
    public boolean isRaw() {
        return true;
    }

}
