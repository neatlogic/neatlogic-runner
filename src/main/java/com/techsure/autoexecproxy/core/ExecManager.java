package com.techsure.autoexecproxy.core;

import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.threadpool.CommonThreadPool;

/**
 * @author lvzk
 * @since 2021/4/21 17:22
 **/
public class ExecManager {

    public static void exec(CommandVo commandVo){
        ExecProcessCommand processCommand = new ExecProcessCommand(commandVo);
        CommonThreadPool.execute(processCommand);
    }

    public static void pause(){

    }

    public static void stop(){

    }

    public static void redo(){

    }

}
