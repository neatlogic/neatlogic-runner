package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentConfigGetFailedException extends ApiRuntimeException {
    public TagentConfigGetFailedException(String message) {
        super("tagent获取配置失败,tagent报错信息：" + message);
    }

    public TagentConfigGetFailedException() {
        super("tagent获取配置失败");
    }
}
