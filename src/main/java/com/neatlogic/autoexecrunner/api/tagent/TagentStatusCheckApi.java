package com.neatlogic.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerFactory;
import org.springframework.stereotype.Service;

@Service
public class TagentStatusCheckApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "检查tagent 连接状态";
    }

    @Override
    public String getToken() {
        return "tagent/status/check";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "检查tagent 连接状态,心跳存在则true，否则false")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = null;
        TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.STATUS_CHECK.getValue());
        result = tagentAction.execute(jsonObj);
        return result;

    }


}
