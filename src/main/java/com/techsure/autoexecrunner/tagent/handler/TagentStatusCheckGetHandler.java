package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.exception.tagent.TagentNotFoundChannelException;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagentStatusCheckGetHandler extends TagentHandlerBase {

    Logger logger = LoggerFactory.getLogger(TagentStatusCheckGetHandler.class);

    @Override
    public String getName() {
        return TagentAction.STATUS_CHECK.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        String ipPort = param.getString("ip") +":"+ param.getString("port");
        if(!Constant.tagentMap.containsKey(ipPort)){
            throw new TagentNotFoundChannelException(ipPort);
        }
        return null;
    }
}
