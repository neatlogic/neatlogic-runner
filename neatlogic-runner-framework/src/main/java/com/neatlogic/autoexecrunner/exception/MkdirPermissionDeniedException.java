package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class MkdirPermissionDeniedException extends ApiRuntimeException {

    public MkdirPermissionDeniedException() {
        super("没有创建文件夹权限");
    }


}
