package com.techsure.autoexecproxy.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.AuthenticateType;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.dto.RestVo;
import com.techsure.autoexecproxy.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lvzk
 * @since 2021/4/21 17:12
 **/
public class ExecProcessCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ExecProcessCommand.class);
    private final ProcessBuilder builder;
    private final CommandVo commandVo;

    public ExecProcessCommand(CommandVo commandVo) {
        this.commandVo = commandVo;
        builder = new ProcessBuilder(commandVo.getCommandList());
        Map<String, String> env = builder.environment();
    }

    @Override
    public void run() {
        try {
            Process process;
            boolean isStarted = false;
            synchronized (commandVo) {
                builder.redirectOutput(new File("C:\\Users\\89770\\Desktop\\codedriver项目\\logs\\log.txt"));
                process = builder.start();
                isStarted = true;
            }
            if (isStarted) {
                StringBuilder errorSb = new StringBuilder();
                InputStream errInput = process.getErrorStream();
                BufferedReader errReader = new BufferedReader(new InputStreamReader(errInput));
                String line = null;
                int length = 0;
                while ((line = errReader.readLine()) != null) {
                    length += line.length();
                    if (length > 4000) {
                        errorSb.append(line.substring(0, length - 4000));
                        break;
                    }
                    errorSb.append(line).append("\n");
                }
                process.waitFor();
                int exitStatus = process.exitValue();
                JSONObject payload = new JSONObject();
                payload.put("jobId", commandVo.getJobId());
                payload.put("jobPhaseUk", commandVo.getJobPhaseUk());
                payload.put("status", 1);
                payload.put("command", commandVo);
                if (exitStatus != 0) {
                    payload.put("status", 0);
                    payload.put("errorMsg", errorSb.toString());
                    logger.error("execute " + commandVo.toString() + " failed. " + errorSb.toString());
                }
                String url = "/codedriver/api/autoexec/job/process/status/update";
                try {
                    JSONObject.parseObject(RestUtil.sendRequest(new RestVo(url, payload, AuthenticateType.BASIC.getValue(), "codedriver", "123456")));
                } catch (Exception e) {
                    logger.error("do RESTFul api failed,url: #{}", url);
                }
            }
        } catch (Exception e) {
            logger.error("run command failed.", e);
            e.printStackTrace();
        }
    }

}
