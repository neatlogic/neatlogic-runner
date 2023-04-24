package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class MongoDataSourceNotFoundException extends ApiRuntimeException {

    public MongoDataSourceNotFoundException(String tenant) {
        super("找不到租户：'"+tenant + "' mongodb 数据源，请检查neatlogic库mongodb表后，重试");
    }

    public MongoDataSourceNotFoundException() {
        super("找不到 mongodb 数据源，请检查neatlogic库mongodb表后，重试");
    }
}
