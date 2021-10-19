package com.techsure.autoexecrunner.common.tagent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {

    // 存放所有的ChannelHandlerContext,便于推送
    public static Map<String, Map<String, ChannelHandlerContext>> channelContextMap = new ConcurrentHashMap<String, Map<String, ChannelHandlerContext>>();

    public static Map<String, ChannelHandlerContext> tagentMap = new ConcurrentHashMap<>();

    public static Map<String, String> runnerGroupMap = new ConcurrentHashMap<>();

    public static Map<String, String> tagentIpMap = new ConcurrentHashMap<>();

    // 存放某一类的channel
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // tagent注册API接口
    public static String ACTION_REGISTER_TAGENT = "tagent/register";

    // tagent更新连接状态接口
    public static String ACTION_UPDATE_TAGENT = "tagent/status/update";

    // tagent更新数据接口
    public static String ACTION_UPDATE_TAGENT_INFO = "tagent/info/update";

    // tagent更新密钥接口
    public static String ACTION_UPDATE_CRED = "tagent/cred/update";

    // tagent版本升级
    public static String ACTION_UPGRADE_TAGENT = "upgradeTagentApi";

    // tagent版本定时更新
    public static String ACTION_REFRESH_TAGENT = "refreshTagentApi";

}
