/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.constvalue;

public enum DeployCiGitlabAuthMode {
    ACCESS_TOKEN("access_token"),
    PRIVATE_TOKEN("private_token"),
    ;
    private final String value;

    DeployCiGitlabAuthMode(String _value) {
        this.value = _value;
    }

    public String getValue() {
        return value;
    }


}
