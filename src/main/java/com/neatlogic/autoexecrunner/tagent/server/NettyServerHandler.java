package com.neatlogic.autoexecrunner.tagent.server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import com.neatlogic.autoexecrunner.common.tagent.Constant;
import com.neatlogic.autoexecrunner.common.tagent.NettyUtil;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.neatlogic.autoexecrunner.exception.tagent.TagentNettyTenantIsNullException;
import com.neatlogic.autoexecrunner.exception.ConnectRefusedException;
import com.neatlogic.autoexecrunner.threadpool.tagent.HeartbeatThreadPool;
import com.neatlogic.autoexecrunner.util.RestUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Scope("prototype")
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    private static final AttributeKey<Integer> AGENT_LISTEN_PORT_KEY = AttributeKey.valueOf("listenPort");
    private static final AttributeKey<String> AGENT_LISTEN_TENANT_KEY = AttributeKey.valueOf("tenant");

    //业务ip，为了方便开墙
    private static final AttributeKey<String> MGMT_IP_KEY = AttributeKey.valueOf("mgmtIp");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.flush();
                ctx.channel().close();
                ctx.close();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        HeartbeatThreadPool.execute(new HeartbeatHandler(ctx, msg));
    }

    /**
     * 客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        agentInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        agentInactive(ctx);
        log.error(cause.getMessage());
    }

    private void agentInactive(ChannelHandlerContext ctx) {
        String agentIp = NettyUtil.getConnectInfo(ctx, "remote")[0];
        Integer listenPort = ctx.channel().attr(AGENT_LISTEN_PORT_KEY).get();
        String tenant = ctx.channel().attr(AGENT_LISTEN_TENANT_KEY).get();
        String mgmtIp = ctx.channel().attr(MGMT_IP_KEY).get();
        //优先使用mgmtIp
        if(StringUtils.isNotBlank(mgmtIp)){
            agentIp = mgmtIp;
        }
        ctx.flush();
        ctx.channel().close();
        ctx.close();

        if (StringUtils.isNotBlank(agentIp) && listenPort != null) {
            Constant.tagentMap.remove(tenant + agentIp + ":" + listenPort);

            //String runnerIp = NettyUtil.getConnectInfo(ctx, "local")[0];
            Map<String, String> params = new HashMap<>();
            params.put("ip", agentIp);
            params.put("port", listenPort.toString());
            //params.put("runnerIp", runnerIp);
            params.put("status", "disconnected");

            Map<String, String> header = new HashMap<>();
            if (TagentConfig.AUTH_TYPE != null && !TagentConfig.AUTH_TYPE.equals("")) {
                if (TagentConfig.AUTH_TYPE.equalsIgnoreCase("basic")) {
                    String key = TagentConfig.ACCESS_KEY + ":" + TagentConfig.ACCESS_SECRET;
                    header.put("Authorization", "Basic " + Base64.encodeBase64String(key.getBytes(StandardCharsets.UTF_8), false));
                    header.put("x-access-date", Long.toString(System.currentTimeMillis()));
                }
            }
            String result = StringUtils.EMPTY;
            JSONObject resultJson = new JSONObject();
            RestVo restVo = null;
            String url = String.format("%s/public/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_UPDATE_TAGENT);
            try {
                restVo = new RestVo(url, AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));
                restVo.setTenant(tenant);
                restVo.setUsername(Config.ACCESS_KEY());
                restVo.setPassword(Config.ACCESS_SECRET());
                result = RestUtil.sendRequest(restVo);
                resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new TagentActionFailedException(url + ":" + resultJson.getString("Message"));
                }
            } catch (JSONException ex) {
                throw new ConnectRefusedException(url + " " + result);
            }
        }
    }

    private static class HeartbeatHandler implements Runnable {
        private final ChannelHandlerContext ctx;
        private final String msg;

        public HeartbeatHandler(ChannelHandlerContext _ctx, String _msg) {
            this.ctx = _ctx;
            this.msg = _msg;
        }

        @Override
        public void run() {
            boolean status = true;
            String agentKey = null;
            JSONObject result = new JSONObject();
            String errorString = "";

            try {
                String agentIp = NettyUtil.getConnectInfo(ctx, "remote")[0];
                if (StringUtils.isBlank(agentIp)) {
                    throw new RuntimeException("无法从 ChannelHandlerContext 获取 agent IP ");
                }

                if (msg != null && ("null".equals(msg) || msg.startsWith("[") && msg.endsWith("]") || msg.startsWith("{") && msg.endsWith("}"))) {
                    JSONObject agentData = JSONObject.parseObject(msg);
                    //优先使用mgmtIp
                    if (agentData.containsKey("mgmtIp") && StringUtils.isNotBlank(agentData.getString("mgmtIp"))) {
                        agentIp = agentData.getString("mgmtIp");
                        ctx.channel().attr(MGMT_IP_KEY).set(agentIp);
                    }
                    Integer listenPort = agentData.getInteger("port");
                    agentKey = agentIp + ":" + listenPort;
                    ctx.channel().attr(AGENT_LISTEN_PORT_KEY).set(listenPort);
                    log.info("received heartbeat from " + agentKey);
                    if (agentData.getString("type").equals("monitor")) {
                        agentData.put("ip", agentIp);
                        Map<String, String> params = JSONObject.parseObject(agentData.toJSONString(), new TypeReference<Map<String, String>>() {});
                        //conf文件缺少tenant配置的情况，异常抛在tagent端
                        if (StringUtils.isBlank(params.get("tenant"))) {
                            throw new TagentNettyTenantIsNullException();
                        }
                        Constant.tagentMap.put(params.get("tenant") + agentKey, ctx);
                        ctx.channel().attr(AGENT_LISTEN_TENANT_KEY).set(params.get("tenant"));
                        params.put("status", "connected");
                        //String groupId = agentData.getString("runnerGroupId");
                        //String groupInfo = agentData.getString("runnerGroup");
                        String tagentId = agentData.getString("agentId");
                        String ipString = agentData.getString("ipString");
                        /*
                         * if (!Constant.runnerGroupMap.containsKey(groupId) ||
                         * StringUtils.isBlank(Constant.runnerGroupMap.get(groupId)) ||
                         * !StringUtils.equals(groupInfo, Constant.runnerGroupMap.get(groupId))) {
                         * params.put("needUpdateGroup", "1"); }
                         */

                        if (!Constant.tagentIpMap.containsKey(tagentId) || StringUtils.isBlank(Constant.tagentIpMap.get(tagentId)) || !StringUtils.equals(ipString, Constant.tagentIpMap.get(tagentId))) {
                            params.put("needUpdateTagentIp", "1");
                        }

                        String agentActionExecRes = StringUtils.EMPTY;
                        JSONObject resultJson = new JSONObject();
                        RestVo restVo = null;
                        restVo = new RestVo(String.format("%s/public/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_UPDATE_TAGENT_INFO), AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));// 调用neatlogic的Tagent信息更新接口
                        restVo.setTenant(params.get("tenant"));
                        restVo.setUsername(Config.ACCESS_KEY());
                        restVo.setPassword(Config.ACCESS_SECRET());
                        agentActionExecRes = RestUtil.sendRequest(restVo);
                        resultJson = JSONObject.parseObject(agentActionExecRes);
                        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                            throw new TagentActionFailedException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                        }
                        if (agentActionExecRes != null && ("null".equals(agentActionExecRes) || agentActionExecRes.startsWith("[") && agentActionExecRes.endsWith("]") || agentActionExecRes.startsWith("{") && agentActionExecRes.endsWith("}"))) {
                            JSONObject resObj = JSONObject.parseObject(agentActionExecRes);
                            JSONObject groupData = resObj.getJSONObject("Return").getJSONObject("Data");
                            if (groupData != null) {
                                // Constant.runnerGroupMap.put(groupId, groupData.optString("groupInfo"));
                                result = groupData;
                            }
                        } else {
                            log.error(String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), Constant.ACTION_UPDATE_TAGENT_INFO) + "返回的数据不是json格式，参数：" + agentData + "，返回值：" + agentActionExecRes);
                        }

                        Constant.tagentIpMap.put(tagentId, agentData.getString("ipString"));
                    }
                } else {
                    log.error(agentIp + "返回的数据不是json格式");
                }
            } catch (Exception e) {
                status = false;
                log.error(agentKey + " Channel信息处理异常", e);
                errorString = e.getMessage();
            }

            result.put("Status", status ? "OK" : "ERROR:" + errorString);
            ctx.writeAndFlush(result + "\n");
        }

    }

}
