package com.neatlogic.autoexecrunner.exception.job;

import com.neatlogic.autoexecrunner.exception.core.UserMessageException;
import com.neatlogic.autoexecrunner.util.RunnerMessages;

/** 保留管道诊断码，同时为已知原因提供当前语言的提示。 */
public class FifoWriteException extends UserMessageException {
    private static final long serialVersionUID = 1L;

    public FifoWriteException(String code) {
        this(code, null, null);
    }

    public FifoWriteException(String code, Throwable cause) {
        this(code, null, cause);
    }

    public FifoWriteException(String code, int errno) {
        this(code, errno, null);
    }

    private FifoWriteException(String code, Integer errno, Throwable cause) {
        super("FIFO " + code + (errno == null ? "" : " (errno=" + errno + ")"), userMessage(code, errno), cause);
    }

    /** 只把白名单内的诊断码和系统错误编号提供给调用方。 */
    private static String userMessage(String code, Integer errno) {
        String reason = reason(code);
        if (reason == null) {
            return null;
        }
        return errno == null ? RunnerMessages.t("runnerframework.error.fifo.withcode", reason, code)
                : RunnerMessages.t("runnerframework.error.fifo.witherrno", reason, code, errno.toString());
    }

    /** 将管道操作的失败原因转换为可读提示。 */
    private static String reason(String code) {
        switch (code) {
            case "ENXIO":
                return RunnerMessages.t("runnerframework.error.fifo.noreader");
            case "EPIPE":
                return RunnerMessages.t("runnerframework.error.fifo.readerclosed");
            case "ENOENT":
                return RunnerMessages.t("runnerframework.error.fifo.missingpath");
            case "EACCES":
                return RunnerMessages.t("runnerframework.error.fifo.accessdenied");
            case "NOT_FIFO":
                return RunnerMessages.t("runnerframework.error.fifo.invalidpipe");
            case "TIMEOUT":
                return RunnerMessages.t("runnerframework.error.fifo.writetimeout");
            case "EAGAIN":
            case "EWOULDBLOCK":
                return RunnerMessages.t("runnerframework.error.fifo.notwritable");
            case "INTERRUPTED":
            case "EINTR":
                return RunnerMessages.t("runnerframework.error.fifo.interrupted");
            case "NATIVE_UNAVAILABLE":
                return RunnerMessages.t("runnerframework.error.fifo.nativeunavailable");
            case "IO_ERROR":
                return RunnerMessages.t("runnerframework.error.fifo.ioerror");
            default:
                return null;
        }
    }

}
