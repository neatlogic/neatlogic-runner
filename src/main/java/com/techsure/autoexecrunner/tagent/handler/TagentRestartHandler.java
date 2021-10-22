package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.exception.tagent.TagentNotFoundChannelException;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagentRestartHandler extends TagentHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(TagentRestartHandler.class);

    @Override
    public String getName() {
        return TagentAction.RESTART.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        JSONObject result = new JSONObject();
        StringBuilder allTagentKeys = new StringBuilder();
        String tagentKey = param.getString("ip") + ":" + param.getString("port");
        if (Constant.tagentMap.containsKey(tagentKey)) {
            ChannelHandlerContext context = Constant.tagentMap.get(tagentKey);
            context.channel().writeAndFlush(param.toString() + "\n");
            result.put("Data", "send command succeed");
        } else {
            for (String s : Constant.tagentMap.keySet()) {
                allTagentKeys.append(s).append(",");
            }
            logger.error("can not find channel for key" + tagentKey + ",keyList:" + allTagentKeys);
            throw new TagentNotFoundChannelException(tagentKey);
        }
        return result;
    }
}
