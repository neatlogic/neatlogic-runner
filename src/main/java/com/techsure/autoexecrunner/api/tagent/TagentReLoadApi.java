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
public class TagentReLoadApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "重启tagent";
    }

    @Override
    public String getToken() {
        return "tagent/reload";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "tagent 重启")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = null;
        TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.RELOAD.getValue());
        result = tagentAction.execute(jsonObj);
        return result;

    }
}
