package com.techsure.autoexecrunner.tagent.server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.common.config.TagentConfig;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.common.tagent.NettyUtil;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.exception.tagent.TagentActionFailedException;
import com.techsure.autoexecrunner.exception.tagent.TagentRunnerConnectRefusedException;
import com.techsure.autoexecrunner.threadpool.tagent.HeartbeatThreadPool;
import com.techsure.autoexecrunner.util.RestUtil;
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
@ChannelHandler.Sharable//待问  可共享
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    private static final AttributeKey<Integer> AGENT_LISTEN_PORT_KEY = AttributeKey.valueOf("listenPort");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {//用户事件触发
        if (evt instanceof IdleStateEvent) { //如果evt是IdleStateEvent的实例类，则强转
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {//如果状态为空闲
                ctx.flush();
                ctx.channel().close();
                ctx.close();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {  //线程池
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

    private void agentInactive(ChannelHandlerContext ctx) {  //实例化？
        String agentIp = NettyUtil.getConnectInfo(ctx, "remote")[0]; // remote？远程
        Integer listenPort = ctx.channel().attr(AGENT_LISTEN_PORT_KEY).get();

        ctx.flush();
        ctx.channel().close();
        ctx.close();

        if (StringUtils.isNotBlank(agentIp) && listenPort != null) {
            Constant.tagentMap.remove(agentIp + ":" + listenPort);

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
            try {
                restVo = new RestVo(String.format("%s/public/api/rest/%s", Config.CODEDRIVER_ROOT(), Constant.ACTION_UPDATE_TAGENT), AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));// 调用codedriver的Tagent状态更新接口
                restVo.setTenant(Config.CODEDRIVER_TENANT());
                restVo.setUsername(Config.ACCESS_KEY());
                restVo.setPassword(Config.ACCESS_SECRET());
                result = RestUtil.sendRequest(restVo);
                resultJson = JSONObject.parseObject(result);
                if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                    throw new TagentActionFailedException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                }
            } catch (Exception ex) {
                assert restVo != null;
                throw new TagentRunnerConnectRefusedException(restVo.getUrl() + " " + result);
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

            try {
                String agentIp = NettyUtil.getConnectInfo(ctx, "remote")[0];
                if (StringUtils.isBlank(agentIp)) {
                    throw new RuntimeException("无法从 ChannelHandlerContext 获取 agent IP ");
                }

                if (msg != null && ("null".equals(msg) || msg.startsWith("[") && msg.endsWith("]") || msg.startsWith("{") && msg.endsWith("}"))) { //判断是否为json数据
                    JSONObject agentData = JSONObject.parseObject(msg);

                    Integer listenPort = agentData.getInteger("port");
                    agentKey = agentIp + ":" + listenPort;
                    ctx.channel().attr(AGENT_LISTEN_PORT_KEY).set(listenPort);
                    Constant.tagentMap.put(agentKey, ctx);
                    log.info("received heartbeat from " + agentKey);
                    if (agentData.getString("type").equals("monitor")) {  //monitor ? 监控
                        agentData.put("ip", agentIp);
                        Map<String, String> params = JSONObject.parseObject(agentData.toJSONString(), new TypeReference<Map<String, String>>() {
                        });//jsonObject转为map
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
                            params.put("needUpdateTagentIp", "1"); //没懂为什么是1
                        }

                        String agentActionExecRes = StringUtils.EMPTY;
                        JSONObject resultJson = new JSONObject();
                        RestVo restVo = null;
                        restVo = new RestVo(String.format("%s/public/api/rest/%s", Config.CODEDRIVER_ROOT(), Constant.ACTION_UPDATE_TAGENT_INFO), AuthenticateType.BASIC.getValue(), JSONObject.parseObject(JSON.toJSONString(params)));// 调用codedriver的Tagent信息更新接口
                        restVo.setTenant(Config.CODEDRIVER_TENANT());
                        restVo.setUsername(Config.ACCESS_KEY());
                        restVo.setPassword(Config.ACCESS_SECRET());
                        agentActionExecRes = RestUtil.sendRequest(restVo);
                        resultJson = JSONObject.parseObject(agentActionExecRes);
                        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                            throw new TagentActionFailedException(restVo.getUrl() + ":" + resultJson.getString("Message"));
                        }
                        if (agentActionExecRes != null && ("null".equals(agentActionExecRes) || agentActionExecRes.startsWith("[") && agentActionExecRes.endsWith("]") || agentActionExecRes.startsWith("{") && agentActionExecRes.endsWith("}"))) {//判断是否为json数据
                            JSONObject resObj = JSONObject.parseObject(agentActionExecRes);
                            JSONObject groupData = resObj.getJSONObject("Return").getJSONObject("Data");
                            if (groupData != null) {
                                // Constant.runnerGroupMap.put(groupId, groupData.optString("groupInfo"));
                                result = groupData;
                            }
                        } else {
                            log.error(String.format("%s/api/rest/%s", Config.CODEDRIVER_ROOT(), Constant.ACTION_UPDATE_TAGENT_INFO) + "返回的数据不是json格式，参数：" + agentData + "，返回值：" + agentActionExecRes);
                        }

                        Constant.tagentIpMap.put(tagentId, agentData.getString("ipString"));
                    }
                } else {
                    log.error(agentIp + "返回的数据不是json格式");
                }
            } catch (Exception e) {
                status = false;
                log.error(agentKey + " Channel信息处理异常", e);
            }

            result.put("Status", status ? "OK" : "ERROR");
            ctx.writeAndFlush(result + "\n");
        }

    }

}
