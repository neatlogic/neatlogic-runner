package com.neatlogic.autoexecrunner.exception.file;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class FileIsNotDirectoryException extends ApiRuntimeException {

    private static final long serialVersionUID = 8038649391411401680L;

    public FileIsNotDirectoryException(String filePath) {
        super(filePath + "不是目录");
    }
}
