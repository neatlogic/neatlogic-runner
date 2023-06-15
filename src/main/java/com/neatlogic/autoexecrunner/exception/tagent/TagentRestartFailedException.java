package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentRestartFailedException extends ApiRuntimeException {
    public TagentRestartFailedException() {
        super("tagent重启失败 ");
    }
}
