package com.neatlogic.autoexecrunner.exception.job;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecuteJobActionException extends ApiRuntimeException {

    public ExecuteJobActionException() {
        super("执行命令失败");
    }

}
