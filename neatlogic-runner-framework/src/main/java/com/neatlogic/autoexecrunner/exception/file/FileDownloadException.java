package com.neatlogic.autoexecrunner.exception.file;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class FileDownloadException extends ApiRuntimeException {

    private static final long serialVersionUID = -319822116765131279L;

    public FileDownloadException(String filePath) {
        super("文件：" + filePath + " 下载失败");
    }
}
