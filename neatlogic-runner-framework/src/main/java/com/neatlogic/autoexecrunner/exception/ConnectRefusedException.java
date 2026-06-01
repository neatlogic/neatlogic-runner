package com.neatlogic.autoexecrunner.exception;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ConnectRefusedException extends ApiRuntimeException {
    public ConnectRefusedException(String s) {
        super("url： '" + s + "' connect failed");
    }
}
