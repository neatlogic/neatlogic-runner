package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.constvalue.TagentAction;
import com.neatlogic.autoexecrunner.exception.tagent.TagentNotFoundChannelAndReloadFieldException;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.tagent.TagentHandlerBase;
import com.neatlogic.autoexecrunner.util.RC4Util;
import com.techsure.tagent.client.TagentClient;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagentReloadHandler extends TagentHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(TagentReloadHandler.class);

    @Override
    public String getName() {
        return TagentAction.RELOAD.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        JSONObject result = new JSONObject();
        StringBuilder allTagentKeys = new StringBuilder();
        String tenant = TenantContext.get().getTenantUuid();
        String tagentKey = param.getString("ip") + ":" + param.getString("port");
        if (Constant.tagentMap.containsKey(tenant + tagentKey)) {
            ChannelHandlerContext context = Constant.tagentMap.get(tenant + tagentKey);
            context.channel().writeAndFlush(param.toString() + "\n");
            result.put("Data", "send command succeed");
        } else {
            String credential = RC4Util.decrypt(param.getString("credential"));
            TagentClient tagentClient = new TagentClient(param.getString("ip"), Integer.parseInt(param.getString("port")), credential, 3000, 30000);
            try {
                tagentClient.reload();
            } catch (Exception e) {
                for (String s : Constant.tagentMap.keySet()) {
                    allTagentKeys.append(s).append(",");
                }
                logger.error("can not find channel for key" + tagentKey + ",keyList:" + allTagentKeys + ", so we couldn't restart through heartbeat. We tried to use password to connect to agent, but failed", e);
                throw new TagentNotFoundChannelAndReloadFieldException(tagentKey);
            }
        }
        return result;
    }
}
