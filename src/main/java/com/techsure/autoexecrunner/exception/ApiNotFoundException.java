package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class ApiNotFoundException extends ApiRuntimeException {

    private static final long serialVersionUID = -8529977350164125804L;

    public ApiNotFoundException(String msg) {
        super(msg);
    }

}
