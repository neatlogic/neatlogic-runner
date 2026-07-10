package com.neatlogic.autoexecrunner.exception.autoexec;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecRToolExecuteFailedException extends ApiRuntimeException {
    private static final long serialVersionUID = 5873509741085846890L;

    public ExecRToolExecuteFailedException(String message) {
        super(message);
    }
}
