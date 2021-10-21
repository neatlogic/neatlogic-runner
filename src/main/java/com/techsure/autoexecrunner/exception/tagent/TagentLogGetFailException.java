package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentLogGetFailException extends ApiRuntimeException {
    public TagentLogGetFailException(String message) {
        super("tagent获取日志失败,tagent报错信息：" + message);
    }
}
