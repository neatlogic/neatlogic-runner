package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentPkgNotFoundException extends ApiRuntimeException {
    public TagentPkgNotFoundException(String fileName) {
        super("runner找不到名为：" + fileName + "的安装包");
    }
}
