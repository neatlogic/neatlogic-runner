package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentDownloadFailedException extends ApiRuntimeException {
    public TagentDownloadFailedException() {
        super("下载文件失败");
    }
}
