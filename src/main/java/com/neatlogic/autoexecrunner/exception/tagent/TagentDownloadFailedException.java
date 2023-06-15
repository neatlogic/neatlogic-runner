package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentDownloadFailedException extends ApiRuntimeException {
    public TagentDownloadFailedException() {
        super("下载文件失败");
    }
}
