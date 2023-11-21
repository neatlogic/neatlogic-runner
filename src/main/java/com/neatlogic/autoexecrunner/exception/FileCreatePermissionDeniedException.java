package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class FileCreatePermissionDeniedException extends ApiRuntimeException {

    public FileCreatePermissionDeniedException() {
        super("没有创建文件权限");
    }


}
