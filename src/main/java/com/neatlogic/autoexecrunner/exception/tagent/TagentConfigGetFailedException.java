package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentConfigGetFailedException extends ApiRuntimeException {
    public TagentConfigGetFailedException() {
        super("获取配置失败");
    }
}
