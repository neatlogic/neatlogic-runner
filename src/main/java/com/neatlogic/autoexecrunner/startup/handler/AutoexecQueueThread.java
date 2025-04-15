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
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AutoexecQueueThread implements IStartUp {
    private static final BlockingQueue<Process> processQueue = new LinkedBlockingQueue<>(Config.MAX_PROCESS_EXECUTE_COUNT() + 5);
    private static final Logger logger = LoggerFactory.getLogger(AutoexecQueueThread.class);
    private static final NeatLogicUniqueBlockingQueue<CommandVo> blockingQueue = new NeatLogicUniqueBlockingQueue<>(Config.MAX_PROCESS_QUEUE_SIZE());
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
                    System.out.println("autoexec job thread is down，ready to start...");
                    logger.debug("autoexec job thread is down，ready to start...");
                    workerThread = new Thread(() -> {
                        Thread.currentThread().setName("AutoexecQueueThread");
                        System.out.println("autoexec job thread start succeed!");
                        while (running) {
                            CommandVo commandVo = null;
                            try {
                                // 你的业务逻辑
                                if (processQueue.size() <= Config.MAX_PROCESS_EXECUTE_COUNT()) {
                                    commandVo = blockingQueue.take();
                                    logger.debug("current autoexec sub process count:{} <= {},autoexec job:{} will create...", processQueue.size(), Config.MAX_PROCESS_EXECUTE_COUNT(), (commandVo.getTenant() + "-" + commandVo.getJobId() + "-" + (MapUtils.isNotEmpty(commandVo.getPassThroughEnv()) ? commandVo.getPassThroughEnv().getString("groupSort") : StringUtils.EMPTY)));
                                    createSubProcessAndStart(commandVo);
                                } else {
                                    logger.debug("autoexec sub process limit count ：{}, need to wait process finish，then keep on creating sub process！", Config.MAX_PROCESS_EXECUTE_COUNT());
                                }
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                logger.error(String.format("autoexec job thread is interrupted...params：%s ,errorMsg:%s%n", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage()), e);
                                break;
                            } catch (Exception e) {
                                logger.error(String.format("create sub process failed：params：%s ,errorMsg:%s", commandVo != null ? JSON.toJSONString(commandVo) : StringUtils.EMPTY, e.getMessage()), e);
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
        Process process;
        try {
            payload.put("jobId", commandVo.getJobId());
            payload.put("status", 1);
            payload.put("command", commandVo);
            payload.put("passThroughEnv", commandVo.getPassThroughEnv().toJSONString());
            process = builder.start();
            String jobName = (commandVo.getTenant() + "-" + commandVo.getJobId() + "-" + (MapUtils.isNotEmpty(commandVo.getPassThroughEnv()) ? commandVo.getPassThroughEnv().getString("groupSort") : StringUtils.EMPTY));
            addProcess(process);
            logger.debug("autoexec job sub process {} is running, pid {},added to processQueue,now processQueue size is {}", jobName, getPid(process), processQueue.size());
            CachedThreadPool.execute(new ProcessWaitTask(process, commandVo, payload, jobName));
        } catch (IOException e) {
            logger.error(String.format("autoexec job sub process start failed: %s ,error: %s", payload, e.getMessage()), e);
        }
    }

    // 兼容不同JDK版本的PID获取
    private long getPid(Process p) {
        try {
            if (p.getClass().getName().contains("UNIXProcess")) {
                Field pidField = p.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                return pidField.getLong(p);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return -1; // 未知PID
    }

    public void stop() {
        running = false;
    }

    public static boolean addCommand(CommandVo commandVo) {
        commandVo.setFcd(new Date());
        return blockingQueue.offer(commandVo);
    }

    public static void addProcess(Process process) {
        boolean result = processQueue.offer(process);
        if (!result) {
            logger.error("processQueue offer failed!current queue size is :{}", processQueue.size());
        }
    }

    public static void removeProcess(Process process, String jobName, Long pid) {
        boolean result = processQueue.remove(process);
        if (!result) {
            logger.error("autoexec process:{} (pid:{})finished. processQueue remove failed!current queue size is :{}", jobName, pid, blockingQueue.size());
        } else {
            logger.debug("autoexec process:{} (pid:{})finished. processQueue remove succeed!current queue size is :{}", jobName, pid, blockingQueue.size());
        }
    }

    public static Integer getProcessQueueSize() {
        return processQueue.size();
    }

    public static Integer getBlockingQueueSize() {
        return blockingQueue.size();
    }

    public static List<CommandVo> getBlockingQueueByJobIdAndGroupSort(String jobId, Integer groupSort) {
        List<CommandVo> list = blockingQueue.getQueue();
        List<CommandVo> jobCommandList = new ArrayList<>();
        for (CommandVo commandVo : list) {
            if (Objects.equals(commandVo.getJobId(), jobId) && (groupSort == null || commandVo.getJobGroupIdList().contains(groupSort))) {
                jobCommandList.add(commandVo);
            }
        }
        return jobCommandList;
    }
}
