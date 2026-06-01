package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentUpgradeFailedException extends ApiRuntimeException {
    public TagentUpgradeFailedException(String message) {
        super("tagent报错信息：" + message);
    }
}
