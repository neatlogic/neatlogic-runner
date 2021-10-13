package com.techsure.autoexecrunner.tagent.server;

import com.techsure.autoexecrunner.common.config.TagentConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class NettyServer {
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Resource
    private NettyServerInitializer nettyServerInitializer;

    @PostConstruct
    public void serverStart() throws InterruptedException {
        Thread t = new Thread(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup();//可理解为一个线程池，内部维护了一组线程，每个线程负责处理多个Channel上的事件，而一个Channel只对应于一个线程
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap(); //netty入口
                b.group(bossGroup, workerGroup);
                b.channel(NioServerSocketChannel.class);
                b.childHandler(nettyServerInitializer);
                b.option(ChannelOption.SO_BACKLOG, 128); //backlog参数指定了队列的大小
                b.childOption(ChannelOption.SO_KEEPALIVE, true);
                b.childOption(ChannelOption.SO_RCVBUF, 1024 * 1024 * 2); //操作接收缓冲区和发送缓冲区　的大小
                ChannelFuture f = b.bind(TagentConfig.AUTOEXEC_NETTY_PORT).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("netty server create error", e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
        t.start();
    }
}
