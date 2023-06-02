package com.techsure.autoexecrunner.codehub.exception;

public class LockFailedException extends RuntimeException {
    
    public LockFailedException(String message) {
        super(message);
    }
    
    public LockFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
