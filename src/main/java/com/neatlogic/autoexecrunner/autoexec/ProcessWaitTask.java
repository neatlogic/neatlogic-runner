package com.neatlogic.autoexecrunner.autoexec;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.NeatLogicThread;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.startup.handler.AutoexecQueueThread;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;

public class ProcessWaitTask extends NeatLogicThread {
    private static final Logger logger = LoggerFactory.getLogger(ProcessWaitTask.class);
    private final Process process;
    private final CommandVo commandVo;
    private final JSONObject payload;

    public ProcessWaitTask(Process process, CommandVo commandVo, JSONObject payload) {
        super("THREAD-AUTOEXEC-WAIT-" + commandVo.getTenant() + "-" + commandVo.getJobId() + "-" + (MapUtils.isNotEmpty(commandVo.getPassThroughEnv()) ? commandVo.getPassThroughEnv().getString("groupSort") : StringUtils.EMPTY));
        this.process = process;
        this.commandVo = commandVo;
        this.payload = payload;
    }


    @Override
    protected void execute() {

        try {
            int exitCode = process.waitFor();
            int exitStatus = process.exitValue();
            commandVo.setExitValue(exitStatus);
            logger.debug("进程[{}] 退出，状态码: {}", getPid(process), exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(String.format("等待被中断: 入参：%s, errorMsg:%s", payload, e.getMessage()), e);
        } finally {
            // 确保关闭流
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            closeQuietly(process.getOutputStream());
            AutoexecQueueThread.removeProcess(process);
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
