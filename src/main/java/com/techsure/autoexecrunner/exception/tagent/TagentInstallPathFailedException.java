package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentInstallPathFailedException extends ApiRuntimeException {
    public TagentInstallPathFailedException() {
        super("获取tagent安装路径失败");
    }
}
