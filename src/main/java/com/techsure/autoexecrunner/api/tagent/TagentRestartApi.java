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
public class TagentRestartApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "重启tagent";
    }

    @Override
    public String getToken() {
        return "tagent/restart";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
            @Param(name = "credential", type = ApiParamType.STRING, desc = "tagent 密码", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "tagent 重启")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = null;
        try {
            TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.RESTART.getValue());
            result = tagentAction.execute(jsonObj);
        } catch (Exception e) {
            result.put("Data", "exec tagent saveConfig failed ， " + e.getMessage());
        }
        return result;

    }


}
