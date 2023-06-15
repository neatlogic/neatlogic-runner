package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class ParamIrregularException extends ApiRuntimeException {
    private static final long serialVersionUID = -6753541026185329206L;

    public ParamIrregularException(String msg) {
        super(msg);
    }
}
