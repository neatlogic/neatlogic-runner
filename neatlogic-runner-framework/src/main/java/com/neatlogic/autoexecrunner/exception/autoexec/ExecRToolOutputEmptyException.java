package com.neatlogic.autoexecrunner.exception.autoexec;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecRToolOutputEmptyException extends ApiRuntimeException {
    private static final long serialVersionUID = 9137608728235639638L;

    public ExecRToolOutputEmptyException(int exitValue) {
        super("execrtool returned empty output, exitValue: " + exitValue);
    }
}
