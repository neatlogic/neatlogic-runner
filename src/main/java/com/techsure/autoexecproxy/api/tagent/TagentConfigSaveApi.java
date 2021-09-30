package com.techsure.autoexecproxy.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.constvalue.TagentAction;
import com.techsure.autoexecproxy.restful.annotation.Description;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.tagent.TagentHandlerBase;
import com.techsure.autoexecproxy.tagent.TagentHandlerFactory;
import org.springframework.stereotype.Service;

@Service
public class TagentConfigSaveApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return null;
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
            @Param(name = "credential", type = ApiParamType.STRING, desc = "tagent 密码", isRequired = true),
            @Param(name = "data", type = ApiParamType.STRING, desc = "tagent 配置", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "tagent 保存配置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        try {
            TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.SAVECONFIG.getValue());
            result = tagentAction.execute(jsonObj);
        } catch (Exception e) {
            result.put("Status", "ERROR");
            result.put("Data", "");
            result.put("Message", "exec tagent saveConfig failed ， " + e.getMessage());
        }
        return result.getJSONObject("Data");
    }

    @Override
    public String getToken() {
        return "tagent/config/save";
    }
}
