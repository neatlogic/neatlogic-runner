package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentInstallPathFailedException extends ApiRuntimeException {
    public TagentInstallPathFailedException() {
        super("获取tagent安装路径失败");
    }
}
