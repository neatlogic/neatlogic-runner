package com.neatlogic.autoexecrunner.tagent.server;

import com.neatlogic.autoexecrunner.common.config.TagentConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private NettyServerHandler nettyServerHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();//  ChannelPipleline 是 ChannelHandler 的管理容器
        // read write all 超时配置
        pipeline.addLast(new IdleStateHandler(TagentConfig.AUTOEXEC_NETTY_READ_TIMEOUT, TagentConfig.AUTOEXEC_NETTY_WRITE_TIMEOUT, TagentConfig.AUTOEXEC_NETTY_ALL_TIMEOUT, TimeUnit.SECONDS));
        // 信息长度与分隔符,换行 \r\n 或 \r
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(1024 * 1024 * 1024, Delimiters.lineDelimiter()));   //framer ?
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(nettyServerHandler);
    }

}
