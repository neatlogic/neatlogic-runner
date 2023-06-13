package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentLogGetFailedException extends ApiRuntimeException {
    public TagentLogGetFailedException() {
        super("获取日志失败");
    }
}
