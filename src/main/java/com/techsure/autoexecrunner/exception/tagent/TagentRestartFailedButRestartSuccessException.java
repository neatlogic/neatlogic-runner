package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentRestartFailedButRestartSuccessException extends ApiRuntimeException {
    public TagentRestartFailedButRestartSuccessException() {
        super("重置密码成功，但是重启失败，可能需要重启tagent服务");
    }
}
