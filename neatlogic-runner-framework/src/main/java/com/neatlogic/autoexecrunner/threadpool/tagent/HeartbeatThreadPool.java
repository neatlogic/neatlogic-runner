package com.neatlogic.autoexecrunner.threadpool.tagent;

import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatThreadPool {
    private static final Log logger = LogFactory.getLog(HeartbeatThreadPool.class);
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger(1);

    /**
     * 心跳任务包含同步网络请求，因此使用独立线程池，避免与自动化执行任务互相占用线程。
     * 核心线程允许超时，在无心跳任务时可回收全部工作线程，保留 CachedThreadPool 的按需回收特性。
     */
    private static final ThreadPoolExecutor heartbeatThreadPool = createThreadPool();

    private static ThreadPoolExecutor createThreadPool() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                TagentConfig.AUTOEXEC_NETTY_HEARTBEAT_MAX_THREADS,
                TagentConfig.AUTOEXEC_NETTY_HEARTBEAT_MAX_THREADS,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(TagentConfig.AUTOEXEC_NETTY_HEARTBEAT_QUEUE_CAPACITY),
                new HeartbeatThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    public static void execute(Runnable command) {
        try {
            heartbeatThreadPool.execute(command);
        } catch (RejectedExecutionException ex) {
            logger.error(String.format(
                    "心跳线程池已满，activeCount=%s, poolSize=%s, queueSize=%s, maxThreadCount=%s",
                    heartbeatThreadPool.getActiveCount(),
                    heartbeatThreadPool.getPoolSize(),
                    heartbeatThreadPool.getQueue().size(),
                    heartbeatThreadPool.getMaximumPoolSize()
            ), ex);
        }
    }

    private static class HeartbeatThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("HEARTBEAT-HANDLER-" + THREAD_SEQUENCE.getAndIncrement());
            return thread;
        }
    }
}
