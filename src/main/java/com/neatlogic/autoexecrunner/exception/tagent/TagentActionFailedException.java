package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentActionFailedException extends ApiRuntimeException {
    public TagentActionFailedException(String message) {
        super("未知异常：" + message);
    }
}
