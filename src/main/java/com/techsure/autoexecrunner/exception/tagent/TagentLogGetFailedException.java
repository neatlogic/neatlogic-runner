package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentLogGetFailedException extends ApiRuntimeException {
    public TagentLogGetFailedException() {
        super("获取日志失败");
    }
}
