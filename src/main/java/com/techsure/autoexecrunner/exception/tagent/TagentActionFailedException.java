package com.techsure.autoexecrunner.exception.tagent;

public class TagentActionFailedException extends RuntimeException {
    public TagentActionFailedException(String message) {
        super("tagent报错信息：" + message);
    }
}
