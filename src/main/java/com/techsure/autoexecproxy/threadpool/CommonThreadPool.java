package com.techsure.autoexecproxy.threadpool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.*;

public class CommonThreadPool {
	private static final Log logger = LogFactory.getLog(CommonThreadPool.class);
	private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
	private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 30, 30, TimeUnit.SECONDS, workQueue, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("COMMON-THREADPOOL-" + t.getName());
			return t;
		}
	}, new ThreadPoolExecutor.AbortPolicy());

	public static void execute(Runnable command) {
		try {
			threadPoolExecutor.execute(command);
		} catch (RejectedExecutionException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public static void invokeAll(List<Callable<Object>> commandList) {
		try {
			threadPoolExecutor.invokeAll(commandList);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static int getWorkQueueSize() {
		return workQueue.size();
	}
	
	public static int getThreadActiveCount(){
		return threadPoolExecutor.getActiveCount();
	}
}
