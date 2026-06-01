package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TagentNotFoundChannelException extends ApiRuntimeException {
    public TagentNotFoundChannelException(String tagentKey) {
        super("不存在 tagent：" + tagentKey + " 的心跳");
    }
}
