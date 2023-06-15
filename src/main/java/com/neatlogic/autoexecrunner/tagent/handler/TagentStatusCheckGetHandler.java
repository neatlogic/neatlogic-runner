package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.exception.tagent.TagentNotFoundChannelException;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
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
        String ipPort = param.getString("ip") + ":" + param.getString("port");
        if (!Constant.tagentMap.containsKey(TenantContext.get().getTenantUuid() + ipPort)) {
            throw new TagentNotFoundChannelException(ipPort);
        }
        return null;
    }
}
