package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentCredUpdateFailedException extends ApiRuntimeException {

    public TagentCredUpdateFailedException(String message) {
        super("tagent重置密码失败,tagent报错信息：" + message);
    }
}
