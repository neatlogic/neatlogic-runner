package com.techsure.autoexecrunner.exception;

public class ConnectRefusedException extends RuntimeException {
    public ConnectRefusedException(String s) {
        super("url： '" + s + "' connect failed");
    }
}
