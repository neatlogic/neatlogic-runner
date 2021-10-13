package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import com.techsure.autoexecrunner.util.RC4Util;
import com.techsure.tagent.client.TagentClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagentLogGetHandler extends TagentHandlerBase {

    private Logger logger = LoggerFactory.getLogger(TagentLogGetHandler.class);

    @Override
    public String getName() {
        return TagentAction.GETLOGS.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        boolean status = true;
        String data = "";
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        try {
            String credential = RC4Util.decrypt(param.getString("credential").substring(4));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.parseInt(param.getString("port")), credential, 3000, 30000);

            TagentResultHandler handler = new TagentResultHandler();
            String osType = tagentClient.getAgentOsType();
            tagentClient.execCmd("echo %TAGENT_HOME%", null, 10000, handler);
            String path = JSONObject.parseObject(handler.getContent()).getJSONArray("std").getString(0).substring(0, 2);

            TagentResultHandler listHandler = new TagentResultHandler();
            int execStatus = 0;
            if ("windows".equals(osType)) {
                execStatus = tagentClient.execCmd(path + "&& cd $TAGENT_HOME/logs && dir /B *.log*", null, 10000, listHandler);
            } else {
                execStatus = tagentClient.execCmd("cd $TAGENT_HOME/logs/ && ls *.log*", null, 10000, listHandler);
            }
            if (execStatus == 0) {
                status = true;
                data = listHandler.getContent();
            } else {
                status = false;
                execInfo.append("get log list falied");
            }
        } catch (Exception e) {
            status = false;
            execInfo.append("exec getlogs cmd error ,exception :  " + e.getMessage());
            logger.error("exec getlogs cmd error ,exception :  " + ExceptionUtils.getStackTrace(e));
        }

        result.put("Data", data);
        return result;
    }
}