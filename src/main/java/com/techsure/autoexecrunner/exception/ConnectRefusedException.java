package com.techsure.autoexecrunner.exception;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class ConnectRefusedException extends ApiRuntimeException {
    public ConnectRefusedException(String s) {
        super("url： '" + s + "' connect failed");
    }
}
