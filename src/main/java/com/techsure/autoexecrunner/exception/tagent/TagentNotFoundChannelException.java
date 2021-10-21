package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentNotFoundChannelException extends ApiRuntimeException {
    public TagentNotFoundChannelException(String tagentKey) {
        super("tagent重启失败,tagent当前所有心跳信息中没有：" + tagentKey + "心跳");
    }
}
