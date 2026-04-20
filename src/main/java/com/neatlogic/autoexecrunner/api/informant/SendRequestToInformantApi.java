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
    private static final int DEFAULT_INFORMANT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_INFORMANT_READ_TIMEOUT = 30000;

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
            @Param(name = "connectTimeout", type = ApiParamType.INTEGER, desc = "连接超时，单位毫秒"),
            @Param(name = "readTimeout", type = ApiParamType.INTEGER, desc = "读取超时，单位毫秒"),
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
        Integer connectTimeout = jsonObj.getInteger("connectTimeout");
        Integer readTimeout = jsonObj.getInteger("readTimeout");

        if ("https".equalsIgnoreCase(protocol)) {
            if (httpsPort == null) {
                throw new ApiRuntimeException("httpsPort不能为空");
            }
            return sendRequest("https", ip, httpsPort, url, method, param, header, connectTimeout, readTimeout);
        }
        if ("http".equalsIgnoreCase(protocol)) {
            if (httpPort == null) {
                throw new ApiRuntimeException("httpPort不能为空");
            }
            return sendRequest("http", ip, httpPort, url, method, param, header, connectTimeout, readTimeout);
        }
        throw new ApiRuntimeException("protocol不合法");
    }

    private Object sendRequest(String protocol, String ip, Integer port, String url, String method, JSONObject param, JSONObject header, Integer connectTimeout, Integer readTimeout) {
        String finalUrl = protocol + "://" + ip + ":" + port + (url.startsWith("/") ? url : "/" + url);
        HttpRequestUtil httpUtil = buildHttpRequest(finalUrl, method, param, header, connectTimeout, readTimeout);
        httpUtil.sendRequest();
        if (httpUtil.getError() != null) {
            throw new ApiRuntimeException(httpUtil.getError());
        }
        return httpUtil.getResult();
    }

    private HttpRequestUtil buildHttpRequest(String finalUrl, String method, JSONObject param, JSONObject header, Integer connectTimeout, Integer readTimeout) {
        HttpRequestUtil httpUtil;
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
        httpUtil.addHeader("Connection", "close");
        httpUtil.setConnectTimeout(connectTimeout != null ? connectTimeout : DEFAULT_INFORMANT_CONNECT_TIMEOUT);
        httpUtil.setReadTimeout(readTimeout != null ? readTimeout : DEFAULT_INFORMANT_READ_TIMEOUT);
        return httpUtil;
    }

    @Override
    public String getToken() {
        return "informant/data/get";
    }

}
