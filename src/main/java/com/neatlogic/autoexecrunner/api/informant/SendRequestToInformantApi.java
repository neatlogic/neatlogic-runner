package com.neatlogic.autoexecrunner.api.informant;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

@Service
public class SendRequestToInformantApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "从neatlogic请求informant数据";
    }


    @Input({
            @Param(name = "url", type = ApiParamType.STRING, desc = "url", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "informant ip", isRequired = true),
            @Param(name = "httpPort", type = ApiParamType.INTEGER, desc = "informant http 端口"),
            @Param(name = "httpsPort", type = ApiParamType.INTEGER, desc = "informant https 端口"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议", isRequired = true),
            @Param(name = "method", type = ApiParamType.STRING, desc = "请求方法", isRequired = true),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "参数"),
            @Param(name = "header", type = ApiParamType.JSONOBJECT, desc = "请求头", isRequired = true)
    })
    @Output({
            @Param(name = "port", type = ApiParamType.JSONOBJECT, desc = "tagent 配置", isRequired = true)
    })
    @Description(desc = "从neatlogic请求informant数据")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String url = jsonObj.getString("url");
        String ip = jsonObj.getString("ip");
        Integer httpPort = jsonObj.getInteger("httpPort");
        Integer httpsPort = jsonObj.getInteger("httpsPort");
        String protocol = jsonObj.getString("protocol");
        String method = jsonObj.getString("method");
        JSONObject param = jsonObj.getJSONObject("param");
        JSONObject header = jsonObj.getJSONObject("header");

        if ("https".equalsIgnoreCase(protocol)) {
            if (httpsPort == null) {
                throw new ApiRuntimeException("httpsPort不能为空");
            }
            return sendRequest("https", ip, httpsPort, url, method, param, header);
        }
        if ("http".equalsIgnoreCase(protocol)) {
            if (httpPort == null) {
                throw new ApiRuntimeException("httpPort不能为空");
            }
            return sendRequest("http", ip, httpPort, url, method, param, header);
        }
        throw new ApiRuntimeException("protocol不合法");
    }

    private Object sendRequest(String protocol, String ip, Integer port, String url, String method, JSONObject param, JSONObject header) throws Exception {
        HttpRequestUtil httpUtil;
        String finalUrl = protocol + "://" + ip + ":" + port + (url.startsWith("/") ? url : "/" + url);
        if (method.equalsIgnoreCase("get")) {
            httpUtil = HttpRequestUtil.get(finalUrl);
            if (MapUtils.isNotEmpty(param)) {
                httpUtil.setQueryString(param);
            }
        } else {
            httpUtil = HttpRequestUtil.post(finalUrl);
            if (MapUtils.isNotEmpty(param)) {
                httpUtil.setFormData(param);
            }
        }
        httpUtil.setContentType(HttpRequestUtil.ContentType.CONTENT_TYPE_APPLICATION_FORM);
        if (MapUtils.isNotEmpty(header)) {
            for (String headerKey : header.keySet()) {
                httpUtil.addHeader(headerKey, header.get(headerKey).toString());
            }
        }

        httpUtil.sendRequest();
        if (httpUtil.getError() != null) {
            throw new ApiRuntimeException(httpUtil.getError());
        }
        return httpUtil.getResult();
    }

    @Override
    public String getToken() {
        return "informant/data/get";
    }

}
