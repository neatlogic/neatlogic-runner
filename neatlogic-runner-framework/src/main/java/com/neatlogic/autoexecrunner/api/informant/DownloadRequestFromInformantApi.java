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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class DownloadRequestFromInformantApi extends PrivateBinaryStreamApiComponentBase {
    private static final int DEFAULT_INFORMANT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_INFORMANT_READ_TIMEOUT = 5 * 60 * 1000;
    private static final String DUMP_TXT_DOWNLOAD_URL = "/api/threaddump/dumptxt/download";
    private static final String LOG_DOWNLOAD_URL = "/api/query/v1/logs/download";

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

    static void validateDownloadUrl(String url) {
        // 下载代理仅允许已确认的 Informant 文件接口，避免被用于访问任意地址。
        if (!StringUtils.equalsAny(url, DUMP_TXT_DOWNLOAD_URL, LOG_DOWNLOAD_URL)) {
            throw new ApiRuntimeException("informant下载地址不允许");
        }
    }

    private void download(String protocol, String ip, Integer port, String url, String method, JSONObject param, JSONObject header, Integer connectTimeout, Integer readTimeout, String downloadName, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String finalUrl = protocol + "://" + ip + ":" + port + url;
        int actualConnectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_INFORMANT_CONNECT_TIMEOUT;
        int actualReadTimeout = readTimeout != null ? readTimeout : DEFAULT_INFORMANT_READ_TIMEOUT;
        if (!response.containsHeader("Content-Disposition")) {
            String userAgent = StringUtils.defaultString(request.getHeader("User-Agent"));
            String fileName = StringUtils.isBlank(userAgent) ? downloadName : FileUtil.getEncodedFileName(userAgent, downloadName);
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        }
        response.setContentType("application/octet-stream");
        HttpRequestUtil httpUtil = buildHttpRequest(finalUrl, method, param, header, actualConnectTimeout, actualReadTimeout, response);
        httpUtil.sendRequest();
        if (httpUtil.getError() != null) {
            writeForwardError(response, httpUtil);
        }
    }

    private HttpRequestUtil buildHttpRequest(String finalUrl, String method, JSONObject param, JSONObject header, int connectTimeout, int readTimeout, HttpServletResponse response) throws Exception {
        HttpRequestUtil httpUtil = HttpRequestUtil.download(
                        finalUrl,
                        method.toUpperCase(),
                        new NonClosingOutputStream(response.getOutputStream()))
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
        httpUtil.setConnectTimeout(connectTimeout);
        // readTimeout also covers the HTTPS SSL handshake read after TCP connect succeeds.
        httpUtil.setReadTimeout(readTimeout);
        return httpUtil;
    }

    private void writeForwardError(HttpServletResponse response, HttpRequestUtil httpUtil) throws IOException {
        response.reset();
        String body;
        if (httpUtil.getResponseCode() > 0) {
            int statusCode = httpUtil.getResponseCode();
            response.setStatus(statusCode >= 400 && statusCode <= 599 ? statusCode : 502);
            response.setContentType(safeErrorContentType(getContentType(httpUtil)));
            body = StringUtils.defaultIfBlank(
                    httpUtil.getErrorResponseBody(),
                    errorBody("informant_http_error", "Informant 节点返回 HTTP 错误"));
        } else {
            String detail = StringUtils.defaultString(httpUtil.getErrorMsg()) + " "
                    + StringUtils.defaultString(httpUtil.getError());
            boolean timeout = StringUtils.containsIgnoreCase(detail, "timed out")
                    || StringUtils.containsIgnoreCase(detail, "timeout");
            response.setStatus(timeout ? 504 : 502);
            response.setContentType("application/json");
            body = errorBody(
                    timeout ? "informant_timeout" : "informant_unreachable",
                    timeout ? "请求 Informant 节点超时" : "无法连接 Informant 节点");
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
        response.flushBuffer();
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

    static String safeErrorContentType(String contentType) {
        if (StringUtils.startsWithIgnoreCase(contentType, "application/json")
                || StringUtils.startsWithIgnoreCase(contentType, "application/problem+json")
                || StringUtils.startsWithIgnoreCase(contentType, "text/")) {
            return contentType;
        }
        return "application/json";
    }

    private String errorBody(String code, String message) {
        JSONObject error = new JSONObject();
        error.put("code", code);
        error.put("message", message);
        JSONObject body = new JSONObject();
        body.put("error", error);
        return body.toJSONString();
    }

    /** HttpRequestUtil 不拥有 Servlet 响应流，失败时仍需由转发层写入结构化错误。 */
    private static class NonClosingOutputStream extends FilterOutputStream {
        NonClosingOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        public void close() {
            // 由 Servlet 容器管理底层响应流。
        }
    }

    @Override
    public String getToken() {
        return "informant/data/download";
    }
}
