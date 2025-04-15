package com.neatlogic.autoexecrunner.exception.job;


import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;

public class JobQueueFullException extends ApiRuntimeException {

    public JobQueueFullException() {
        super("作业队列已满(当前队列数" + AutoexecQueueThread.getBlockingQueueSize() + ">= 配置队列最大数" + Config.MAX_PROCESS_QUEUE_SIZE() + ")，无法执行");
    }

}
