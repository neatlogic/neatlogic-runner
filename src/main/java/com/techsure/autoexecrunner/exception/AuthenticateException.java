package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class AuthenticateException extends ApiRuntimeException {

    private static final long serialVersionUID = -8107366715550277893L;

    public AuthenticateException(String role) {
        super(role);
    }
}
