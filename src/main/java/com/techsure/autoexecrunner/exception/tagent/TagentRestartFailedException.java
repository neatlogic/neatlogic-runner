package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentRestartFailedException extends ApiRuntimeException {
    public TagentRestartFailedException() {
        super("tagent重启失败 ");
    }
}
