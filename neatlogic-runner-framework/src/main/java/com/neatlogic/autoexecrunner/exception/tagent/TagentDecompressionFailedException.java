package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentDecompressionFailedException extends ApiRuntimeException {
    public TagentDecompressionFailedException() {
        super("安装包解压升级失败");
    }
}
