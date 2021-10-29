package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentDecompressionFailedException extends ApiRuntimeException {
    public TagentDecompressionFailedException() {
        super("安装包解压升级失败");
    }
}
