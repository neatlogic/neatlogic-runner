package com.techsure.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.constvalue.TagentAction;
import com.techsure.autoexecrunner.tagent.TagentHandlerBase;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
        try {
            String tagentKey = param.getString("ip") + ":" + param.getString("port");
            for (int i = 0; i < Constant.tagentMap.size(); i++) {
                System.out.println((Constant.tagentMap.get(i)));
            }
            if (Constant.tagentMap.containsKey(tagentKey)) {//  Constant.tagentMap  只有匹配到心跳才进去里面
                ChannelHandlerContext context = Constant.tagentMap.get(tagentKey);
                context.channel().writeAndFlush(param.toString() + "\n");
                execInfo.append("send command succeed");
            } else {
                Iterator<String> keylist = Constant.tagentMap.keySet().iterator();
                String allkeys = "";
                while (keylist.hasNext()) {
                    allkeys += keylist.next() + ",";
                }
                logger.error("can not find channel for " + tagentKey + ",keylist:" + allkeys);
                execInfo.append("can not find channel to send command for " + tagentKey);
            }
        } catch (Exception e) {
            execInfo.append("exec reload cmd error ,exception :  " + e.getMessage());
            logger.error("exec reload cmd error ,exception :  " + ExceptionUtils.getStackTrace(e));
        }
        result.put("Data", execInfo.toString());
        return result;
    }
}
