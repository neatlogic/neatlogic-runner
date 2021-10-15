package com.techsure.autoexecrunner.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/4/21 17:12
 **/
public class ExecProcessCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ExecProcessCommand.class);
    private final ProcessBuilder builder;
    private final CommandVo commandVo;
    private static final File NULL_FILE = new File("/dev/null");
    protected UserContext userContext;
    protected TenantContext tenantContext;

    public ExecProcessCommand(CommandVo commandVo) {
        this.userContext = UserContext.get();
        this.tenantContext = TenantContext.get();
        this.commandVo = commandVo;
        builder = new ProcessBuilder(commandVo.getCommandList());
        builder.redirectOutput(NULL_FILE);
        builder.redirectError(NULL_FILE);
        Map<String, String> env = builder.environment();
        env.put("tenant", commandVo.getTenant());
    }

    @Override
    public void run() {
        TenantContext.init(tenantContext);
        UserContext.init(userContext);
        JSONObject payload = new JSONObject();
        String result = null;
        try {
            Process process;
            synchronized (commandVo) {
                payload.put("jobId", commandVo.getJobId());
                payload.put("status", 1);
                payload.put("command", commandVo);
                //builder.redirectOutput(new File("C:\\Users\\89770\\Desktop\\codedriver项目\\logs\\log.txt"));
                process = builder.start();
                if (Objects.equals(commandVo.getAction(), "abort") || Objects.equals(commandVo.getAction(), "pause")) {
                    process.waitFor();
                }
                int exitStatus = process.exitValue();
                commandVo.setExitValue(exitStatus);
                if (exitStatus != 0 && !(Objects.equals(commandVo.getAction(), "abort") && exitStatus == 143)) {//排除中止已停止的process
                    logger.error("execute " + commandVo.toString() + "exit status:" + exitStatus + ", failed.");
                }
            }
        } catch (Exception e) {
            //TODO 去掉wait for 会抛异常
            logger.error("run command failed.", e);
            payload.put("status", 0);
            payload.put("errorMsg", e.getMessage());
            logger.error("execute " + commandVo.toString() + " failed. " + e.getMessage());
        } finally {
            if (commandVo.getExitValue() == 143 || commandVo.getExitValue() == 0 && (Objects.equals(commandVo.getAction(), "abort") || Objects.equals(commandVo.getAction(), "pause"))) {
                String CALLBACK_PROCESS_UPDATE_URL = "autoexec/job/process/status/update";
                String url = String.format("%s/api/rest/%s", Config.CODEDRIVER_ROOT(), CALLBACK_PROCESS_UPDATE_URL);
                try {
                    result = RestUtil.sendRequest(new RestVo(url, payload, AuthenticateType.BEARER.getValue(), commandVo.getTenant()));
                    JSONObject.parseObject(result);
                } catch (Exception e) {
                    logger.error("do RESTFul api failed,url: #{},result: #{}", url, result);
                }
            }
        }
    }

}
