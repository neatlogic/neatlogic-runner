package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class MongodbException extends ApiRuntimeException {
    public MongodbException() {
        super("mongodb 操作失败，请检查mongodb服务正常后，重试");
    }

}
