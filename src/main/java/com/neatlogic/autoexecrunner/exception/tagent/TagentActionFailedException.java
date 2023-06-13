package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentActionFailedException extends ApiRuntimeException {
    public TagentActionFailedException(String message) {
        super("tagent报错信息：" + message);
    }
}
