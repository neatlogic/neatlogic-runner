package com.neatlogic.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.exception.tagent.TagentNotFoundChannelException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResetTagentCredentialApi extends PrivateApiComponentBase {

    private final Logger logger = LoggerFactory.getLogger(ResetTagentCredentialApi.class);

    @Override
    public String getName() {
        return "重置密码";
    }

    @Override
    public String getToken() {
        return "/tagent/credential/reset";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        jsonObj.put("type", "resetcred");
        JSONObject result = new JSONObject();
        String tenant = TenantContext.get().getTenantUuid();
        String tagentKey = jsonObj.getString("ip") + ":" + jsonObj.getString("port");
        if (Constant.tagentMap.containsKey(tenant + tagentKey)) {
            ChannelHandlerContext context = Constant.tagentMap.get(tenant + tagentKey);
            context.channel().writeAndFlush(jsonObj.toString() + "\n");
            result.put("Data", "send command succeed");
        } else {
            StringBuilder allTagentKeys = new StringBuilder();
            for (String s : Constant.tagentMap.keySet()) {
                allTagentKeys.append(s).append(",");
            }
            logger.error("can not find channel for key" + tagentKey + ",keyList:" + allTagentKeys);
            throw new TagentNotFoundChannelException(tagentKey);
        }
        return result;
    }

}
