package com.neatlogic.autoexecrunner.tagent.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        System.err.println(msg);
    }

    //与runner连接断开
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //尝试建立连接三次 判断连接状态，之后定时连接
        super.channelActive(ctx);
    }
}