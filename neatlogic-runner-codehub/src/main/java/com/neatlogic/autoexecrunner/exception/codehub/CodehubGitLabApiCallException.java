
package com.neatlogic.autoexecrunner.exception.codehub;


import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class CodehubGitLabApiCallException extends ApiRuntimeException {


    public CodehubGitLabApiCallException(String msg) {
        super("调用gitlab出现错误: " + msg);
    }
}
