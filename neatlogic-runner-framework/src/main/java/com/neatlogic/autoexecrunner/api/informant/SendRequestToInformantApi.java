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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SendRequestToInformantApi extends PrivateApiComponentBase {
    static final String FORWARD_RESPONSE_MARKER = "_informantForwardResponse";
    private static final int DEFAULT_INFORMANT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_INFORMANT_READ_TIMEOUT = 5 * 60 * 1000;

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
        int actualConnectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_INFORMANT_CONNECT_TIMEOUT;
        int actualReadTimeout = readTimeout != null ? readTimeout : DEFAULT_INFORMANT_READ_TIMEOUT;
        HttpRequestUtil httpUtil = buildHttpRequest(finalUrl, method, param, header, actualConnectTimeout, actualReadTimeout);
        httpUtil.sendRequest();
        if (httpUtil.getResponseCode() > 0) {
            String body = httpUtil.getResponseCode() >= 400
                    ? httpUtil.getErrorResponseBody()
                    : httpUtil.getResult();
            return buildForwardResponse(httpUtil.getResponseCode(), getContentType(httpUtil), body);
        }
        return buildTransportErrorResponse(httpUtil);
    }

    private HttpRequestUtil buildHttpRequest(String finalUrl, String method, JSONObject param, JSONObject header, int connectTimeout, int readTimeout) {
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
        httpUtil.setConnectTimeout(connectTimeout);
        // readTimeout also covers the HTTPS SSL handshake read after TCP connect succeeds.
        httpUtil.setReadTimeout(readTimeout);
        return httpUtil;
    }

    static JSONObject buildForwardResponse(int statusCode, String contentType, String body) {
        JSONObject response = new JSONObject();
        response.put(FORWARD_RESPONSE_MARKER, true);
        response.put("statusCode", statusCode);
        response.put("contentType", StringUtils.defaultIfBlank(contentType, "application/json"));
        response.put("body", StringUtils.defaultString(body));
        return response;
    }

    static JSONObject buildTransportErrorResponse(HttpRequestUtil httpUtil) {
        String detail = StringUtils.defaultString(httpUtil.getErrorMsg()) + " "
                + StringUtils.defaultString(httpUtil.getError());
        boolean timeout = StringUtils.containsIgnoreCase(detail, "timed out")
                || StringUtils.containsIgnoreCase(detail, "timeout");
        JSONObject error = new JSONObject();
        error.put("code", timeout ? "informant_timeout" : "informant_unreachable");
        error.put("message", timeout ? "请求 Informant 节点超时" : "无法连接 Informant 节点");
        JSONObject body = new JSONObject();
        body.put("error", error);
        return buildForwardResponse(timeout ? 504 : 502, "application/json", body.toJSONString());
    }

    private String getContentType(HttpRequestUtil httpUtil) {
        Map<String, List<String>> headers = httpUtil.getResponseHeadersMap();
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), "Content-Type") && entry.getValue() != null
                    && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "informant/data/get";
    }

}
