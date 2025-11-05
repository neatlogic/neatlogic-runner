package com.neatlogic.autoexecrunner.api.informant;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiAnonymousAccessSupportEnum;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Service
public class SendDownloadRequestToNeatlogicApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "从informant下载neatlogic附件";
    }

    @Override
    public boolean isRaw() {
        return true;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "informant uuid", isRequired = true),
            @Param(name = "url", type = ApiParamType.STRING, desc = "neatlogic接口url(token)", isRequired = true),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "参数"),
            @Param(name = "sign", type = ApiParamType.STRING, desc = "签名", isRequired = true),
            @Param(name = "timestamp", type = ApiParamType.LONG, desc = "时间戳", isRequired = true)
    })
    @Description(desc = "从informant下载neatlogic附件")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = jsonObj.getString("url");
        String sign = jsonObj.getString("sign");
        String uuid = jsonObj.getString("uuid");
        Long timestamp = jsonObj.getLong("timestamp");
        url = String.format("%s/api/binary/%s", Config.NEATLOGIC_ROOT(), url);
        JSONObject param = jsonObj.getJSONObject("param");
        JSONObject requestParam = new JSONObject();
        requestParam.put("sign", sign);
        requestParam.put("timestamp", timestamp);
        requestParam.put("uuid", uuid);
        requestParam.put("param", param);
        String tenant = request.getHeader("Tenant");
        System.out.println("发送下载请求到neatlogic:" + url + ",参数:" + requestParam);
        HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setAuthType(AuthenticateType.HMAC)
                .setTenant(tenant)
                .setOutputStream(response.getOutputStream())
                .setToken(SystemUser.AUTOEXEC.getToken())
                .setUsername(SystemUser.AUTOEXEC.getUserId()).setPayload(requestParam.toString());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            requestUtil.addHeader(name, value);
        }
        requestUtil.sendRequest();
        return null;
    }

    @Override
    public String getToken() {
        return "informant/neatlogic/download";
    }

    //从informant访问neatlogic，支持匿名访问，由neatlogic来校验合法性
    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }

}
