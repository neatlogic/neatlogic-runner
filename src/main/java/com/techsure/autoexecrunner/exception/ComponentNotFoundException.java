package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class ComponentNotFoundException extends ApiRuntimeException {
    private static final long serialVersionUID = -6165807991291970685L;

    public ComponentNotFoundException(String msg) {
        super(msg);
    }

}
