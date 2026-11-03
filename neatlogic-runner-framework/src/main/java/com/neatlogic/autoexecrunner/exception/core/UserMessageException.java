package com.neatlogic.autoexecrunner.exception.core;

/** 带有可公开提示的已知异常，技术消息保留给日志和旧客户端。 */
public class UserMessageException extends ApiRuntimeException {
    private static final long serialVersionUID = 1L;
    private final String userMessage;

    public UserMessageException(String diagnostic, String userMessage, Throwable cause) {
        super(diagnostic, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
