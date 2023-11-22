package com.neatlogic.autoexecrunner.exception.file;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class FileDeleteException extends ApiRuntimeException {

    public FileDeleteException(String filePath) {
        super("文件：" + filePath + " 删除失败");
    }
}
