package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentNotFindChannelException extends ApiRuntimeException {
    public TagentNotFindChannelException(String tagentKey) {
        super("tagent重启失败,tagent当前所有心跳信息中没有：" + tagentKey + "心跳");
    }
}
