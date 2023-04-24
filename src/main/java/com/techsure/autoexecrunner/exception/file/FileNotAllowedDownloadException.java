package com.techsure.autoexecrunner.exception.file;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class FileNotAllowedDownloadException extends ApiRuntimeException {

    private static final long serialVersionUID = 578767690326029076L;

    public FileNotAllowedDownloadException(String filePath) {
        super(filePath + " 不允许下载");
    }
}
