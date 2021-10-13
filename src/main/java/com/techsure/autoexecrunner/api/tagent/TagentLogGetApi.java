package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import com.techsure.autoexecrunner.tagent.TagentHandlerFactory;
import org.springframework.stereotype.Service;

@Service
public class TagentLogGetApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "tagent 获取日志";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
            @Param(name = "credential", type = ApiParamType.STRING, desc = "tagent 密码", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "tagent 获取日志")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        try {
            TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.GETLOGS.getValue());
            result = tagentAction.execute(paramObj);
        } catch (Exception e) {
            result.put("Status", "ERROR");
            result.put("Data", "");
            result.put("Message", "exec tagent getlogs failed ， " + e.getMessage());
        }
        return result.getJSONObject("Data");
    }

    @Override
    public String getToken() {
        return "/tagent/log/get";
    }
}