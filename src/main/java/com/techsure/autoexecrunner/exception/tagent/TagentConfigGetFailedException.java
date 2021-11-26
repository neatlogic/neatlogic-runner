package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentConfigGetFailedException extends ApiRuntimeException {
    public TagentConfigGetFailedException() {
        super("获取配置失败");
    }
}
