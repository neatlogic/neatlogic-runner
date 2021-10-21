package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.exception.tagent.TagentNotFoundChannelException;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class TagentRestartHandler extends TagentHandlerBase {

    private Logger logger = LoggerFactory.getLogger(TagentRestartHandler.class);

    @Override
    public String getName() {
        return TagentAction.RESTART.getValue();
    }

    @Override
    public JSONObject execute(JSONObject param) {
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        String tagentKey = StringUtils.EMPTY;
        String allkeys = "";
        tagentKey = param.getString("ip") + ":" + param.getString("port");
        for (int i = 0; i < Constant.tagentMap.size(); i++) {
            System.out.println((Constant.tagentMap.get(i)));
        }
        if (Constant.tagentMap.containsKey(tagentKey)) {//  Constant.tagentMap  只有匹配到心跳才进去里面
            ChannelHandlerContext context = Constant.tagentMap.get(tagentKey);
            context.channel().writeAndFlush(param.toString() + "\n");
            execInfo.append("send command succeed");
        } else {
            Iterator<String> keylist = Constant.tagentMap.keySet().iterator();
            while (keylist.hasNext()) {
                allkeys += keylist.next() + ",";
            }
            logger.error("can not find channel for " + tagentKey + ",keylist:" + allkeys);
            throw new TagentNotFoundChannelException(tagentKey);
        }
        result.put("Data", execInfo.toString());
        return result;
    }
}
