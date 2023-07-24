package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentDownloadFailedException;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.util.RC4Util;
import com.neatlogic.tagent.client.TagentClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TagentLogDownloadHandler extends TagentHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(TagentLogDownloadHandler.class);

    @Override
    public String getName() {
        return TagentAction.DOWNLOAD_LOG.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        JSONObject result = new JSONObject();
        try {
            String credential = RC4Util.decrypt(param.getString("credential"));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.parseInt(param.getString("port")), credential, 3000, 30000);
            TagentResultHandler handler = new TagentResultHandler();
            String path = param.getString("path").replaceAll("\n", "");
            int execStatus = tagentClient.download("$TAGENT_HOME/logs/" + path,  Config.AUTOEXEC_HOME() + File.separator + path, null, false, false, handler);
            if (execStatus == 0) {
                byte[] fileCharArray = handler.getFileByteArray();
                result.put("Data", new String(fileCharArray));
            } else {
                throw new TagentDownloadFailedException();
            }
        } catch (ApiRuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("exec download cmd error ,exception :  " + ExceptionUtils.getStackTrace(e));
            throw new TagentActionFailedException(e.getMessage());
        }
        return result;
    }
}