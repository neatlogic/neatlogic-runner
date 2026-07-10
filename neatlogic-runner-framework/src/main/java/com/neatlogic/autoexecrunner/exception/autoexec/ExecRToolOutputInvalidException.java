package com.neatlogic.autoexecrunner.exception.autoexec;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecRToolOutputInvalidException extends ApiRuntimeException {
    private static final long serialVersionUID = -815894564927883579L;

    public ExecRToolOutputInvalidException(int exitValue, String output, Throwable ex) {
        super("execrtool returned invalid json, exitValue: " + exitValue + ", output: " + output, ex);
    }
}
