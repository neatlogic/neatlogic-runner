package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentClientAuthException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentClientNetException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentConfigGetFailedException;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.util.RC4Util;
import com.neatlogic.tagent.client.TagentClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TagentConfigGetHandler extends TagentHandlerBase {

    Logger logger = LoggerFactory.getLogger(TagentConfigGetHandler.class);

    @Override
    public String getName() {
        return TagentAction.GET_CONFIG.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        String data = "";
        JSONObject result = new JSONObject();
        try {
            String credential = RC4Util.decrypt(param.getString("credential"));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.parseInt(param.getString("port")), credential, 3000, 30000);

            String osType = tagentClient.getAgentOsType();
            TagentResultHandler pathHandler = new TagentResultHandler();
            int execStatus = -1;
            tagentClient.execCmd("echo %TAGENT_HOME% ", null, 10000, pathHandler);
            String path = JSON.parseObject(pathHandler.getContent()).getJSONArray("std").getString(0).trim().replaceAll("\n", "").replaceAll("\\/", "\\\\");
            TagentResultHandler handler = new TagentResultHandler();
            if ("windows".equals(osType)) {
                execStatus = tagentClient.execCmd("type " + path + "/conf/tagent.conf".replaceAll("\\/", "\\\\"), null, 10000, handler);
            } else {
                execStatus = tagentClient.execCmd("cat $TAGENT_HOME/conf/tagent.conf", null, 10000, handler);
            }
            if (execStatus == 0) {
                data = handler.getContent();
            } else {
                throw new TagentConfigGetFailedException();
            }
        } catch (com.neatlogic.tagent.exception.AuthException e) {
            logger.error("exec TagentConfigGet cmd error ,exception : {} " , ExceptionUtils.getStackTrace(e));
            throw new TagentClientAuthException(e.getMessage());
        } catch (IOException e) {
            logger.error("exec TagentConfigGet cmd error ,exception :  {}" , ExceptionUtils.getStackTrace(e));
            throw new TagentClientNetException(e.getMessage());
        } catch (Exception e) {
            logger.error("exec TagentConfigGet cmd error ,exception :  {}" , ExceptionUtils.getStackTrace(e));
            throw new TagentActionFailedException(e.getMessage());
        }
        result.put("Data", data);
        return result;
    }
}
