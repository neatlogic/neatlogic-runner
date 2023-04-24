package com.techsure.autoexecrunner.exception.job;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecuteJobActionException extends ApiRuntimeException {

    public ExecuteJobActionException() {
        super("执行命令失败");
    }

}
