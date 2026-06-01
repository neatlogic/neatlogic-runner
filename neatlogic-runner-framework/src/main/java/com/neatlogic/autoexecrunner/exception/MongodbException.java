package com.neatlogic.autoexecrunner.exception;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class MongodbException extends ApiRuntimeException {
    public MongodbException() {
        super("mongodb 操作失败，请检查mongodb服务正常且neatlogic数据库mongodb表中配置正确后，重试。详情请查看runner错误日志");
    }

}
