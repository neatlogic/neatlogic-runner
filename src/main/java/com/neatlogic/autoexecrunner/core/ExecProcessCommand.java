package com.neatlogic.autoexecrunner.core;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.CommandVo;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.RestUtil;
import com.neatlogic.autoexecrunner.util.TimeUtil;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
                payload.put("passThroughEnv", commandVo.getPassThroughEnv().toJSONString());
                process = builder.start();
                System.out.println(String.join(" ", commandVo.getCommandList()));
                if (Objects.equals(commandVo.getAction(), "abort") || Objects.equals(commandVo.getAction(), "pause")) {
                    process.waitFor();
                } else {
                    process.waitFor(1, TimeUnit.SECONDS);
                }
                int exitStatus = process.exitValue();
                commandVo.setExitValue(exitStatus);
                /*if (exitStatus != 0) {//排除中止已停止的process
                    InputStreamReader reader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(reader, writer);
                    result = writer.toString();
                    logger.error("execute " + commandVo.toString() + "exit status:" + exitStatus + ", failed: "+result);
                }*/
            }
        } catch (IllegalThreadStateException e) {
            //不wait for子进程正常结束返回，就一定会抛 IllegalThreadStateException 异常
            logger.info("execute " + commandVo + " failed. " + e.getMessage(), e);
        } catch (Exception e) {
            payload.put("status", 0);
            payload.put("errorMsg", e.getMessage());
            logger.error("execute " + commandVo.toString() + " failed. " + e.getMessage(), e);
            try {
                FileUtil.saveFile(TimeUtil.getTimeToDateString(System.currentTimeMillis(), TimeUtil.HH_MM_SS) + " ERROR: " + e.getMessage(), commandVo.getConsoleLogPath(), true);
            } catch (Exception ex) {
                logger.error(e.getMessage(), e);
            }
        } finally {
            if (commandVo != null && Objects.equals(commandVo.getExitValue(), 2)) {
                String CALLBACK_PROCESS_UPDATE_URL = "autoexec/job/process/status/update";
                String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), CALLBACK_PROCESS_UPDATE_URL);
                try {
                    result = RestUtil.sendRequest(new RestVo(url, payload, AuthenticateType.HMAC.getValue(), commandVo.getTenant()));
                    JSONObject.parseObject(result);
                } catch (JSONException e) {
                    logger.error("do RESTFul api failed,url: #{},result: #{}", url, result);
                }
            }
        }
    }

}
