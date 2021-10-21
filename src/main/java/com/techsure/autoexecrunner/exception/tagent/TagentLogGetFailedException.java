package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentLogGetFailedException extends ApiRuntimeException {
    public TagentLogGetFailedException(String message) {
        super("tagent获取日志失败,tagent报错信息：" + message);
    }

    public TagentLogGetFailedException() {
        super("tagent获取日志失败");
    }
}
