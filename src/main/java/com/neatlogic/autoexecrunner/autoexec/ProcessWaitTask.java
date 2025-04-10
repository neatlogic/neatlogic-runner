package com.neatlogic.autoexecrunner.autoexec;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.NeatLogicThread;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;

public class ProcessWaitTask extends NeatLogicThread {
    private static final Logger logger = LoggerFactory.getLogger(ProcessWaitTask.class);
    private final Process process;
    private final CommandVo commandVo;
    private final JSONObject payload;
    private final String jobName;

    public ProcessWaitTask(Process process, CommandVo commandVo, JSONObject payload, String jobName) {
        super("THREAD-AUTOEXEC-WAIT-" + jobName);
        this.process = process;
        this.commandVo = commandVo;
        this.payload = payload;
        this.jobName = jobName;
    }


    @Override
    protected void execute() {
        Long pid = null;
        try {
            int exitCode = process.waitFor();
            int exitStatus = process.exitValue();
            commandVo.setExitValue(exitStatus);
            pid = getPid(process);
            logger.debug("process[{}] finished，exitCode: {}", pid, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(String.format("thread interrupt: param：%s, errorMsg:%s", payload, e.getMessage()), e);
        } finally {
            // 确保关闭流
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            closeQuietly(process.getOutputStream());
            AutoexecQueueThread.removeProcess(process, jobName, pid);
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

    private void closeQuietly(java.io.Closeable c) {
        try {
            if (c != null) c.close();
        } catch (IOException ignore) {
        }
    }
}
