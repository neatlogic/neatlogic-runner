package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentCredUpdateFailException extends ApiRuntimeException {

    public TagentCredUpdateFailException(String message) {
        super("tagent重置密码失败,tagent报错信息：" + message);
    }
}
