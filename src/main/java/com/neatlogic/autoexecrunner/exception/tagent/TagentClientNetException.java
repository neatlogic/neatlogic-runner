package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentClientNetException extends ApiRuntimeException {
    public TagentClientNetException(String message) {
        super("tagentClient连接异常：" + message);
    }
}
