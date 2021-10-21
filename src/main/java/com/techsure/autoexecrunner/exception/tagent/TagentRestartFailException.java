package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentRestartFailException extends ApiRuntimeException {
    public TagentRestartFailException(String message) {
        super("tagent重启失败 " + message);
    }
}
