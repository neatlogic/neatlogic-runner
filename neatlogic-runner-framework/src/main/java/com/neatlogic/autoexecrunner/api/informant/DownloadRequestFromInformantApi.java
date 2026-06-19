package com.neatlogic.autoexecrunner.api.informant;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Service
public class DownloadRequestFromInformantApi extends PrivateBinaryStreamApiComponentBase {
    private static final int DEFAULT_INFORMANT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_INFORMANT_READ_TIMEOUT = 30000;
    private static final String DUMP_TXT_DOWNLOAD_URL = "/api/threaddump/dumptxt/download";

    @Override
    public String getName() {
        return "从neatlogic请求informant下载数据";
    }

    @Input({
            @Param(name = "url", type = ApiParamType.STRING, desc = "informant接口url", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "informant ip", isRequired = true),
            @Param(name = "httpPort", type = ApiParamType.INTEGER, desc = "informant http 端口"),
            @Param(name = "httpsPort", type = ApiParamType.INTEGER, desc = "informant https 端口"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议", isRequired = true),
            @Param(name = "method", type = ApiParamType.ENUM, rule = "get,post", desc = "请求方法", isRequired = true),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "参数"),
            @Param(name = "downloadName", type = ApiParamType.STRING, desc = "下载文件名"),
            @Param(name = "connectTimeout", type = ApiParamType.INTEGER, desc = "连接超时，单位毫秒"),
            @Param(name = "readTimeout", type = ApiParamType.INTEGER, desc = "读取超时，单位毫秒"),
            @Param(name = "header", type = ApiParamType.JSONOBJECT, desc = "请求头", isRequired = true)
    })
    @Output({})
    @Description(desc = "从neatlogic请求informant下载数据")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = jsonObj.getString("url");
        validateDownloadUrl(url);
        String ip = jsonObj.getString("ip");
        Integer httpPort = jsonObj.getInteger("httpPort");
        Integer httpsPort = jsonObj.getInteger("httpsPort");
        String protocol = jsonObj.getString("protocol");
        String method = jsonObj.getString("method");
        JSONObject param = jsonObj.getJSONObject("param");
        JSONObject header = jsonObj.getJSONObject("header");
        Integer connectTimeout = jsonObj.getInteger("connectTimeout");
        Integer readTimeout = jsonObj.getInteger("readTimeout");
        String downloadName = StringUtils.defaultIfBlank(jsonObj.getString("downloadName"), "dump.txt");

        if ("https".equalsIgnoreCase(protocol)) {
            if (httpsPort == null) {
                throw new ApiRuntimeException("httpsPort不能为空");
            }
            download("https", ip, httpsPort, url, method, param, header, connectTimeout, readTimeout, downloadName, request, response);
            return null;
        }
        if ("http".equalsIgnoreCase(protocol)) {
            if (httpPort == null) {
                throw new ApiRuntimeException("httpPort不能为空");
            }
            download("http", ip, httpPort, url, method, param, header, connectTimeout, readTimeout, downloadName, request, response);
            return null;
        }
        throw new ApiRuntimeException("protocol不合法");
    }

    private void validateDownloadUrl(String url) {
        if (!StringUtils.equals(url, DUMP_TXT_DOWNLOAD_URL)) {
            throw new ApiRuntimeException("informant下载地址不允许");
        }
    }

    private void download(String protocol, String ip, Integer port, String url, String method, JSONObject param, JSONObject header, Integer connectTimeout, Integer readTimeout, String downloadName, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String finalUrl = protocol + "://" + ip + ":" + port + url;
        if (!response.containsHeader("Content-Disposition")) {
            String userAgent = StringUtils.defaultString(request.getHeader("User-Agent"));
            String fileName = StringUtils.isBlank(userAgent) ? downloadName : FileUtil.getEncodedFileName(userAgent, downloadName);
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        }
        response.setContentType("application/octet-stream");
        HttpRequestUtil httpUtil = buildHttpRequest(finalUrl, method, param, header, connectTimeout, readTimeout, response);
        httpUtil.sendRequest();
        if (httpUtil.getError() != null) {
            throw new ApiRuntimeException(httpUtil.getError());
        }
    }

    private HttpRequestUtil buildHttpRequest(String finalUrl, String method, JSONObject param, JSONObject header, Integer connectTimeout, Integer readTimeout, HttpServletResponse response) throws Exception {
        HttpRequestUtil httpUtil = HttpRequestUtil.download(finalUrl, method.toUpperCase(), response.getOutputStream())
                .setContentType(HttpRequestUtil.ContentType.CONTENT_TYPE_APPLICATION_FORM)
                .setResponseHeaders(Arrays.asList("Content-Type"));
        if ("get".equalsIgnoreCase(method)) {
            if (MapUtils.isNotEmpty(param)) {
                httpUtil.setQueryString(param);
            }
        } else if (MapUtils.isNotEmpty(param)) {
            httpUtil.setFormData(param);
        }
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
        return "informant/data/download";
    }
}
