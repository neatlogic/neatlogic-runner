package com.techsure.autoexecrunner.threadpool.tagent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

public class HeartbeatThreadPool {
    private static final Log logger = LogFactory.getLog(HeartbeatThreadPool.class);
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("HEARTBEAT-HANDLER-" + t.getName());
            return t;
        }
    });

    public static void execute(Runnable command) {
        try {
            cachedThreadPool.execute(command);
        } catch (RejectedExecutionException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

}
