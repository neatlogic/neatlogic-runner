package com.techsure.autoexecproxy.exception;


import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;

public class ComponentNotFoundException extends ApiRuntimeException {
    private static final long serialVersionUID = -6165807991291970685L;

    public ComponentNotFoundException(String msg) {
        super(msg);
    }

}
