package com.techsure.autoexecproxy.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.constvalue.TagentAction;
import com.techsure.autoexecproxy.restful.annotation.Description;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecproxy.tagent.TagentHandlerBase;
import com.techsure.autoexecproxy.tagent.TagentHandlerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class TagentLogGetApi extends PrivateBinaryStreamApiComponentBase {
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
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject result = new JSONObject();
        try {
            TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.GETLOGS.getValue());
            result = tagentAction.execute(paramObj);
        } catch (Exception e) {
            result.put("Status", "ERROR");
            result.put("Data", "");
            result.put("Message", "exec tagent " + paramObj.getString("type") + " failed ， " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getToken() {
        return "/tagent/getlogs";
    }
}
