package com.techsure.autoexecrunner.core;

import com.google.common.collect.Lists;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.JobAction;
import com.techsure.autoexecrunner.dto.CommandVo;
import com.techsure.autoexecrunner.threadpool.CommonThreadPool;
import com.techsure.autoexecrunner.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/21 17:22
 **/
public class ExecManager {
    private static final String COMMAND_AUTOEXEC = "autoexec";

    public static void exec(CommandVo commandVo) throws Exception {
        commandVo.setAction(JobAction.EXEC.getValue());
        //save params.json
        String filePath = Config.AUTOEXEC_HOME() + File.separator + getJobPath(commandVo.getJobId(), new StringBuilder()) + File.separator + "params.json";
        FileUtil.saveFile(commandVo.getConfig(), filePath);
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--paramsfile", filePath);
        commandList = Lists.newArrayList(commandList);
        if (commandVo.getFirstFire() != null && commandVo.getFirstFire()) {
            commandList.add("--firstfire");
        }
        if (commandVo.getNoFireNext() != null && commandVo.getNoFireNext()) {
            commandList.add("--nofirenext");
        }
        if(commandVo.getPassThroughEnv() != null){
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        commandVo.setCommandList(commandList);
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
    }

    /**
     * 递归截取3位jobId作为path
     *
     * @param jobId     作业id
     * @param jobPathSb 根据作业id生产的path
     * @return 作业path
     */
    public static String getJobPath(String jobId, StringBuilder jobPathSb) {
        if (jobPathSb.length() > 0) {
            jobPathSb.append(File.separator);
        }
        if (jobId.length() > 3) {
            String tmp = jobId.substring(0, 3);
            jobId = jobId.replaceFirst(tmp, StringUtils.EMPTY);
            jobPathSb.append(tmp);
            getJobPath(jobId, jobPathSb);
        } else {
            jobPathSb.append(jobId);
        }
        return jobPathSb.toString();
    }

    public static void pause(CommandVo commandVo) {
        commandVo.setAction(JobAction.PAUSE.getValue());
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--pause");
        commandList = Lists.newArrayList(commandList);
        if(commandVo.getPassThroughEnv() != null){
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        commandVo.setCommandList(commandList);
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
    }

    public static void abort(CommandVo commandVo) {
        commandVo.setAction(JobAction.ABORT.getValue());
        //set command
        List<String> commandList = Arrays.asList("autoexec", "--jobid", commandVo.getJobId(), "--execuser", UserContext.get().getUserUuid(), "--abort");
        commandList = Lists.newArrayList(commandList);
        if(commandVo.getPassThroughEnv() != null){
            commandList.add("--passthroughenv");
            commandList.add(commandVo.getPassThroughEnv().toString());
        }
        commandVo.setCommandList(commandList);
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
    }

    public static void reset() {

    }

}
