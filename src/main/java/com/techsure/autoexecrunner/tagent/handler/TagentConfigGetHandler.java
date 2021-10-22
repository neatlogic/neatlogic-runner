package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.exception.tagent.TagentConfigGetFailedException;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import com.techsure.autoexecrunner.util.RC4Util;
import com.techsure.tagent.client.TagentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            String credential = RC4Util.decrypt(param.getString("credential").substring(4));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.parseInt(param.getString("port")), credential, 3000, 30000);

            String osType = tagentClient.getAgentOsType();
            TagentResultHandler pathHandler = new TagentResultHandler();
            int execStatus = -1;
            tagentClient.execCmd("echo %TAGENT_HOME% ", null, 10000, pathHandler);
            String path = JSONObject.parseObject(pathHandler.getContent()).getJSONArray("std").getString(0).trim().replaceAll("\n", "").replaceAll("\\/", "\\\\");
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
        } catch (Exception e) {
            logger.error("执行config命令失败，请求参数：" + param.toString(), e);
            throw new TagentConfigGetFailedException(e.getMessage());
        }
        result.put("Data", data);
        return result;
    }
}
