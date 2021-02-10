package com.techsure.autoexecproxy.exception;


import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;

public class PermissionDeniedException extends ApiRuntimeException {
    private static final long serialVersionUID = 6148939003449322484L;

    public PermissionDeniedException() {
        super("没有权限进行当前操作");
    }


}
