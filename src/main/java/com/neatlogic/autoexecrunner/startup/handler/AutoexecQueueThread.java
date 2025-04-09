package com.neatlogic.autoexecrunner.startup.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.queue.NeatLogicUniqueBlockingQueue;
import com.neatlogic.autoexecrunner.asynchronization.threadpool.CachedThreadPool;
import com.neatlogic.autoexecrunner.autoexec.ProcessWaitTask;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.startup.IStartUp;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AutoexecQueueThread implements IStartUp {
    private static final BlockingQueue<Process> processQueue = new LinkedBlockingQueue<>(Config.MAX_PROCESS_QUEUE_SIZE() + 5);
    private static final Logger logger = LoggerFactory.getLogger(AutoexecQueueThread.class);
    private static final NeatLogicUniqueBlockingQueue<CommandVo> blockingQueue = new NeatLogicUniqueBlockingQueue<>(50000);
    private volatile boolean running = true;

    @Override
    public String getName() {
        return "创建自动化作业线程";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void doService() {
        // 启动监控线程
        Thread watchdog = new Thread(() -> {
            Thread workerThread = null;

            while (running) {
                if (workerThread == null || !workerThread.isAlive()) {
                    System.out.println("工作线程未运行，准备启动...");
                    logger.debug("工作线程未运行，准备启动...");
                    workerThread = new Thread(() -> {
                        Thread.currentThread().setName("");
                        while (running) {
                            CommandVo commandVo = null;
                            try {
                                // 你的业务逻辑
                                if (processQueue.size() <= Config.MAX_PROCESS_QUEUE_SIZE()) {
                                    commandVo = blockingQueue.take();
                                    logger.debug("作业{} 即将运行...", commandVo.getTenant() + "-" + commandVo.getJobId());
                                    createSubProcessAndStart(commandVo);
                                } else {
                                    logger.debug("作业进程最大数量：{}, 需等待运行中的进程结束后，才继续创建队列内的作业进程！", Config.MAX_PROCESS_QUEUE_SIZE());
                                }
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                System.out.printf("创建自动化作业子进程的线程被中断...入参：%s ,errorMsg:%s%n", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage());
                                logger.error(String.format("创建自动化作业子进程的线程被中断...入参：%s ,errorMsg:%s%n", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage()), e);
                                break;
                            } catch (Exception e) {
                                System.out.printf("创建自动化作业子进程失败：入参：%s ,errorMsg:%s%n", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage());
                                logger.error(String.format("创建自动化作业子进程失败：入参：%s ,errorMsg:%s", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage()), e);
                                break; // 退出由外层 watchdog 负责重启
                            }
                        }
                    });

                    workerThread.setDaemon(true);
                    workerThread.start();
                }

                try {
                    Thread.sleep(2000); // 每2秒检查一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void createSubProcessAndStart(CommandVo commandVo) {
        File NULL_FILE = new File("/dev/null");
        ProcessBuilder builder = new ProcessBuilder(commandVo.getCommandList());
        builder.redirectOutput(NULL_FILE);
        File consoleLog = FileUtil.createFile(commandVo.getConsoleLogPath());
        builder.redirectError(ProcessBuilder.Redirect.appendTo(consoleLog));
        Map<String, String> env = builder.environment();
        JSONObject environment = commandVo.getEnvironment();
        if (MapUtils.isNotEmpty(environment)) {
            for (Map.Entry<String, Object> entry : environment.entrySet()) {
                env.put(entry.getKey(), entry.getValue().toString());
            }
        }
        env.put("tenant", commandVo.getTenant());
        JSONObject payload = new JSONObject();
        Process process = null;
        try {
            payload.put("jobId", commandVo.getJobId());
            payload.put("status", 1);
            payload.put("command", commandVo);
            payload.put("passThroughEnv", commandVo.getPassThroughEnv().toJSONString());
            process = builder.start();
            addProcess(process);
            CachedThreadPool.execute(new ProcessWaitTask(process, commandVo, payload));
        } catch (IOException e) {
            logger.error(String.format("进程启动失败: %s ,error: %s", payload, e.getMessage()), e);
            System.err.println("进程启动失败: " + payload + ",error:" + e.getMessage());
        } finally {
            // 确保关闭流
            if (process != null) {
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
                closeQuietly(process.getOutputStream());
            }
        }
    }

    private void closeQuietly(java.io.Closeable c) {
        try {
            if (c != null) c.close();
        } catch (IOException ignore) {
        }
    }

    public void stop() {
        running = false;
    }

    public static void addUpdateTagent(CommandVo commandVo) {
        blockingQueue.offer(commandVo);
    }

    public static void addProcess(Process process) {
        boolean result = processQueue.offer(process);
        if (!result) {
            logger.error("processQueue offer failed!current queue size is :{}", processQueue.size());
        }
    }

    public static void removeProcess(Process process) {
        boolean result = processQueue.remove(process);
        if (!result) {
            logger.error("processQueue remove failed!current queue size is :{}", processQueue.size());
        }
    }
}
