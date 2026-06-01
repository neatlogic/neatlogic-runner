package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class AuthenticateException extends ApiRuntimeException {

    private static final long serialVersionUID = -8107366715550277893L;

    public AuthenticateException(String role) {
        super(role);
    }
}
