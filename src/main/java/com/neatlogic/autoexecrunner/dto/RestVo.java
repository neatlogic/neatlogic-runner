package com.neatlogic.autoexecrunner.dto;


import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RestVo {
    private String id;
    private boolean lazyLoad = false;
    private String url;
    private String authType;
    private String username;
    private String password;
    private String token;
    private String method;
    private String tenant;
    private JSONObject payload;
    private String rawPayload;
    private Charset charset = StandardCharsets.UTF_8;
    List<String> paramNameList;
    List<String> paramValueList;

    private int timeout = 30000;//毫秒

    private List<Object> paramList;
    private Map<String, Object> paramMap;
    public RestVo(){

    }

    public RestVo(String url, JSONObject payload, String authType, String username, String password, String tenant) {
        this.url = url;
        this.authType = authType;
        this.password = password;
        this.username = username;
        this.payload = payload;
        this.tenant = tenant;
    }

    public RestVo(String url, JSONObject payload, String authType, String tenant) {
        this.url = url;
        this.authType = authType;
        this.payload = payload;
        this.tenant = tenant;
        if( UserContext.get() != null) {
            this.token = UserContext.get().getToken();
        }
    }

    public RestVo(String url, String authType, JSONObject payload) {
        this.url = url;
        this.authType = authType;
        this.payload = payload;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, Object> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, Object> paramMap) {
        this.paramMap = paramMap;
    }

    public List<Object> getParamList() {
        return paramList;
    }

    public void setParamList(List<Object> paramList) {
        this.paramList = paramList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isLazyLoad() {
        return lazyLoad;
    }

    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getParamNameList() {
        return paramNameList;
    }

    public void setParamNameList(List<String> paramNameList) {
        this.paramNameList = paramNameList;
    }

    public List<String> getParamValueList() {
        return paramValueList;
    }

    public void setParamValueList(List<String> paramValueList) {
        this.paramValueList = paramValueList;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
