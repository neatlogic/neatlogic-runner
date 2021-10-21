package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentConfigGetFailException extends ApiRuntimeException {
    public TagentConfigGetFailException(String message) {
        super("tagent获取日志失败,tagent报错信息：" + message);
    }
}
