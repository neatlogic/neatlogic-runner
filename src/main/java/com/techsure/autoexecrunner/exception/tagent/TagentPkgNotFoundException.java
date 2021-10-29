package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentPkgNotFoundException extends ApiRuntimeException {
    public TagentPkgNotFoundException(String fileName) {
        super("安装包名为：" + fileName + "的文件没有找到");
    }
}
