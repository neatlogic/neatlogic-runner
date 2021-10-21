package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentConfigSaveFailException extends ApiRuntimeException {
    public TagentConfigSaveFailException(String message) {
        super("tagent保存日志失败,tagent报错信息：" + message);
    }
}
