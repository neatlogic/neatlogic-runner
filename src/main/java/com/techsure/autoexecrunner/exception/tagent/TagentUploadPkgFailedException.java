package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentUploadPkgFailedException extends ApiRuntimeException {
    public TagentUploadPkgFailedException() {
        super("tagent上传安装包失败");
    }
}
