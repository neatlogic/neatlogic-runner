package com.neatlogic.autoexecrunner.constvalue;

/**
 * @Title: AuthenticateType
 * @Description: 认证方式
 * @author: chenqiwei
 * @date: 2021/2/101:29 下午
 **/
public enum AuthenticateType {
    NOAUTH("", "无需认证"),
    BASIC("basic", "Basic认证"),
    BEARER("bearertoken", "Bearer Token"),
    HMAC("hmac", "HMAC认证");
    private final String type;
    private final String text;

    AuthenticateType(String _type, String _text) {
        this.type = _type;
        this.text = _text;
    }

    public String getValue() {
        return this.type;
    }

    public String getText() {
        return this.text;
    }
}
