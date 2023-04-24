/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class HttpMethodIrregularException extends ApiRuntimeException {

    public HttpMethodIrregularException() {
        super("只支持GET和POST两种请求方法");
    }
}
