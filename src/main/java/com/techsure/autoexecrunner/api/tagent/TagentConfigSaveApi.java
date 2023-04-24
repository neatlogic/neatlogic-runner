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
public class TagentConfigSaveApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "保存tagent配置";
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
        TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.SAVE_CONFIG.getValue());
        result = tagentAction.execute(jsonObj);
        return result.getJSONObject("Data");
    }

    @Override
    public String getToken() {
        return "tagent/config/save";
    }
}
