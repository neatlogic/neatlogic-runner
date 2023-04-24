package com.techsure.autoexecrunner.exception.file;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class FileIsNotFileException extends ApiRuntimeException {

    private static final long serialVersionUID = 6130657556707906097L;

    public FileIsNotFileException(String filePath) {
        super(filePath + "不是文件");
    }
}
