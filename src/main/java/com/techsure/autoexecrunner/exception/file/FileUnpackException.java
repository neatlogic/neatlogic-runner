package com.techsure.autoexecrunner.exception.file;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class FileUnpackException extends ApiRuntimeException {

    private static final long serialVersionUID = -7516566869825062989L;

    public FileUnpackException(String filePath, String cmd) {
        super("解压文件：" + filePath + " 失败, " + "解压命令：" + cmd);
    }
}
