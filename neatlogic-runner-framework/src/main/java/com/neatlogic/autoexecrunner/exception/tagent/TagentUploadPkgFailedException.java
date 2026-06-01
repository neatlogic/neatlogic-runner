package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentUploadPkgFailedException extends ApiRuntimeException {
    public TagentUploadPkgFailedException() {
        super("tagent上传安装包失败");
    }
}
