package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class MkdirPermissionDeniedException extends ApiRuntimeException {

    public MkdirPermissionDeniedException() {
        super("该runner的deployadmin账号没有创建文件夹权限");
    }


}
