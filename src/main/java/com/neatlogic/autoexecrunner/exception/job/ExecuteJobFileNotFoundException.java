package com.neatlogic.autoexecrunner.exception.job;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ExecuteJobFileNotFoundException extends ApiRuntimeException {

    public ExecuteJobFileNotFoundException(String path) {
        super("文件：‘"+path+"’ 不存在，下载失败");
    }

}
