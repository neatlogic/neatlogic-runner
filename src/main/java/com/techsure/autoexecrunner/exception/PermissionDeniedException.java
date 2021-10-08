package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class PermissionDeniedException extends ApiRuntimeException {
    private static final long serialVersionUID = 6148939003449322484L;

    public PermissionDeniedException() {
        super("没有权限进行当前操作");
    }


}
