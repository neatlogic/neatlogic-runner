package com.neatlogic.autoexecrunner.common.tagent;


import io.netty.channel.ChannelHandlerContext;

public class NettyUtil {
    public static String[] getConnectInfo(ChannelHandlerContext ctx, String type) {
        if ("local".equals(type)) {
            return ctx.channel().localAddress().toString().substring(1).split(":");
        } else {
            return ctx.channel().remoteAddress().toString().substring(1).split(":");
        }
    }
}