package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import com.techsure.autoexecrunner.util.RC4Util;
import com.techsure.tagent.client.TagentClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TagentConfigSaveHandler extends TagentHandlerBase {

    Logger logger = LoggerFactory.getLogger(TagentConfigSaveHandler.class);

    @Override
    public String getName() {
        return TagentAction.SAVE_CONFIG.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        String data = "";
        JSONObject result = new JSONObject();
        JSONArray dataArray = param.getJSONArray("data");
        String tagentData = StringUtils.EMPTY;
        for (int i = 0; i < dataArray.size(); i++) {
            tagentData = tagentData + dataArray.get(i);
        }
        try {
            String credential = RC4Util.decrypt(param.getString("credential").substring(4));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.valueOf(param.getString("port")), credential, 3000, 30000);

            InputStream input = new ByteArrayInputStream(tagentData.trim().getBytes(StandardCharsets.UTF_8));
            tagentClient.upload(input, "tagent.conf", "$TAGENT_HOME/conf/", null, false);
            tagentClient.reload();
        } catch (Exception e) {
            logger.error("exec saveconfig cmd error ,exception :  " + ExceptionUtils.getStackTrace(e));
            throw new TagentActionFailedException(e.getMessage());
        }
        result.put("Data", data);
        return result;
    }
}
