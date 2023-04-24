package com.techsure.autoexecrunner.exception.file;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class FileNotFoundException extends ApiRuntimeException {

    private static final long serialVersionUID = 5478827250541404509L;

    public FileNotFoundException(String filePath) {
        super(filePath + "不存在");
    }
}
