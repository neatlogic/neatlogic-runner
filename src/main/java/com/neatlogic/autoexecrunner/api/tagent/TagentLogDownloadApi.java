package com.neatlogic.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class TagentLogDownloadApi extends PrivateBinaryStreamApiComponentBase {
    @Override
    public String getName() {
        return "下载tagent 日志";
    }

    @Input({
            @Param(name = "ip", type = ApiParamType.STRING, desc = "tagent ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "tagent 端口", isRequired = true),
            @Param(name = "path", type = ApiParamType.STRING, desc = "tagent log path", isRequired = true),
            @Param(name = "credential", type = ApiParamType.STRING, desc = "tagent 密码", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "tagent 获取日志")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject result = new JSONObject();
        TagentHandlerBase tagentAction = TagentHandlerFactory.getAction(TagentAction.DOWNLOAD_LOG.getValue());
        result.put("Data", tagentAction.execute(paramObj).getString("Data"));
        return result;
    }

    @Override
    public String getToken() {
        return "tagent/log/download";
    }

    @Override
    public boolean isRaw() {
        return true;
    }

}
