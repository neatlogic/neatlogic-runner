package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentClientAuthException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentClientNetException;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.util.RC4Util;
import com.neatlogic.tagent.client.TagentClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        StringBuilder tagentData = new StringBuilder(StringUtils.EMPTY);
        for (Object o : dataArray) {
            tagentData.append(o);
        }
        try {
            String credential = RC4Util.decrypt(param.getString("credential"));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.valueOf(param.getString("port")), credential, 3000, 30000);

            InputStream input = new ByteArrayInputStream(tagentData.toString().trim().getBytes(StandardCharsets.UTF_8));
            tagentClient.upload(input, "tagent.conf", "$TAGENT_HOME/conf/", null, false);
            tagentClient.reload();
        } catch (com.neatlogic.tagent.exception.AuthException e) {
            logger.error("exec TagentConfigSave cmd error ,exception : {} " , ExceptionUtils.getStackTrace(e));
            throw new TagentClientAuthException(e.getMessage());
        } catch (IOException e) {
            logger.error("exec TagentConfigSave cmd error ,exception :  {}" , ExceptionUtils.getStackTrace(e));
            throw new TagentClientNetException(e.getMessage());
        } catch (Exception e) {
            logger.error("exec TagentConfigSave cmd error ,exception :  {}" , ExceptionUtils.getStackTrace(e));
            throw new TagentActionFailedException(e.getMessage());
        }
        result.put("Data", data);
        return result;
    }
}
