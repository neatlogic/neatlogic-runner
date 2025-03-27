package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentClientAuthException extends ApiRuntimeException {
    public TagentClientAuthException(String message) {
        super("tagentClient权限异常：" + message);
    }
}
