package com.techsure.autoexecrunner.codehub.exception;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class LockFailedException extends ApiRuntimeException {
    
    public LockFailedException(String message) {
        super(message);
    }
    
    public LockFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
