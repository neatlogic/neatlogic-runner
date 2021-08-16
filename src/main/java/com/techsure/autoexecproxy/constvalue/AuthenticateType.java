package com.techsure.autoexecproxy.constvalue;

/**
 * @Title: AuthenticateType
 * @Package: com.techsure.autoexecproxy.constvalue
 * @Description: 认证方式
 * @author: chenqiwei
 * @date: 2021/2/101:29 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public enum AuthenticateType {
    NOAUTH("", "无需认证"), BASIC("basic", "Basic认证"), BEARER("bearertoken", "Bearer Token");
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
