package com.techsure.autoexecproxy.core;

import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.JobAction;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.threadpool.CommonThreadPool;
import com.techsure.autoexecproxy.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;

/**
 * @author lvzk
 * @since 2021/4/21 17:22
 **/
public class ExecManager {
    private static final String COMMAND_AUTOEXEC = "autoexec";

    public static void exec(CommandVo commandVo) throws Exception {
        commandVo.setAction(JobAction.EXEC.getValue());
        //save params.json
        String filePath = Config.PARAM_PATH() + File.separator + getJobPath(commandVo.getJobId(),new StringBuilder()) + File.separator + "params.json";
        FileUtil.saveFile(commandVo.getConfig(),filePath,"","");
        //set command
        commandVo.setCommandList(Collections.singletonList(String.format("'python3 /app/autoexec/bin/autoexec --jobid \"%s\" --execuser \"%s\" -p %s'",commandVo.getJobId(),commandVo.getExecUser(),filePath)));
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
    }

    /**
     * 递归截取3位jobId作为path
     * @param jobId 作业id
     * @param jobPathSb 根据作业id生产的path
     * @return 作业path
     */
    public static String getJobPath(String jobId,StringBuilder jobPathSb){
        if(jobPathSb.length() > 0){
            jobPathSb.append(File.separator);
        }
        if(jobId.length() > 3){
            String tmp = jobId.substring(0,3);
            jobId = jobId.replaceFirst(tmp, StringUtils.EMPTY);
            jobPathSb.append(tmp);
            getJobPath(jobId,jobPathSb);
        }else {
            jobPathSb.append(jobId);
        }
        return jobPathSb.toString();
    }

    public static void pause(){

    }

    public static void stop(){

    }

    public static void reset(){

    }

}
