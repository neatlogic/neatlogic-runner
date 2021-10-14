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
import com.techsure.autoexecrunner.service.TagentService;
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

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
            String url = TagentConfig.AUTOEXEC_CODEDRIVER_ROOT + "/" + Constant.ACTION_REGISTER_TAGENT;
            status = tagentService.forwardCodedriverWeb(jsonObj,url,execInfo);
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

}
