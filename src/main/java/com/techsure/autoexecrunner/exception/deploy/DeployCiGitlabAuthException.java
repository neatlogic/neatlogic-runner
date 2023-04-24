/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.exception.deploy;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class DeployCiGitlabAuthException extends ApiRuntimeException {

    public DeployCiGitlabAuthException() {
        super("gitlab认证失败");
    }
}
