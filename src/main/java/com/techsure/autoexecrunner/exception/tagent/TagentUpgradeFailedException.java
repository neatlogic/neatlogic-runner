package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentUpgradeFailedException extends ApiRuntimeException {
    public TagentUpgradeFailedException(String message) {
        super("tagent升级,tagent报错信息：" + message);
    }
}
