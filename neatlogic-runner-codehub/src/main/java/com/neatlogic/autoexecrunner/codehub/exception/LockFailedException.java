package com.neatlogic.autoexecrunner.codehub.exception;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class LockFailedException extends ApiRuntimeException {
    
    public LockFailedException(String message) {
        super(message);
    }
    
    public LockFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
