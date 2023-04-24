package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentActionFailedException extends ApiRuntimeException {
    public TagentActionFailedException(String message) {
        super("tagent报错信息：" + message);
    }
}
