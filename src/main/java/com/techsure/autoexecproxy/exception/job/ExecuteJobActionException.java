package com.techsure.autoexecproxy.exception.job;


import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;

public class ExecuteJobActionException extends ApiRuntimeException {

    public ExecuteJobActionException() {
        super("执行命令失败");
    }

}
